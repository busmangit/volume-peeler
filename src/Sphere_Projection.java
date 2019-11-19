import ij.*;
import ij.process.*;
import ij.plugin.ZProjector;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import java.util.HashMap;
import java.awt.Color;
import java.awt.AWTEvent;
import java.awt.Label;

public class Sphere_Projection implements ExtendedPlugInFilter, DialogListener {

  private int FLAGS = STACK_REQUIRED | DOES_ALL;
  private byte[] sourcePixels;
  private Point[] auxPointsArray;
  private int width, height, nSlices, nFrames, threshold, pixelsPerSlice, pixelsPerFrame;
  private String units;
  private double proportion, calibX, calibY, calibZ;
  private boolean preview = true, okPressed = false;
  private HashMap<Double, ColorProcessor> previewCache;
  private Sphere previewSphere;
  private float[] previewDistanceMap;
  private int previewFrame = 1;
  
  /**
   * This method is called by ImageJ for initialization.
   * @param arg Unused here. For plugins in a .jar file this argument string can
   *            be specified in the plugins.config file of the .jar archive.
   * @param imp The ImagePlus containing the image (or stack) to process.
   * @return    The method returns flags (i.e., a bit mask) specifying the
   *            capabilities (supported formats, etc.) and needs of the filter.
   *            See PlugInFilter.java and ExtendedPlugInFilter in the ImageJ
   *            sources for details.
   */
  public int setup(String arg, ImagePlus imp) {
    Calibration cal = imp.getCalibration(); 
    this.calibX = cal.pixelWidth;
    this.calibY = cal.pixelHeight;
    this.calibZ = cal.pixelDepth;
    this.units = cal.getUnits();
    this.width = imp.getWidth();
    this.height = imp.getHeight();
    this.nFrames = imp.getNFrames();
    this.nSlices = imp.getNSlices();
    this.pixelsPerSlice = this.width * this.height;
    this.pixelsPerFrame = this.pixelsPerSlice * this.nSlices;
    this.previewCache = new HashMap<Double, ColorProcessor>();
    auxPointsArray = new Point[this.pixelsPerFrame];
    initSourcePixels(imp);
    initThreshold(imp);
    runInitialEstimations(imp);
    return FLAGS;
  }

  private void initSourcePixels(ImagePlus imp) {
    this.sourcePixels = new byte[this.pixelsPerFrame * this.nFrames];
    for (int slice = 0; slice < this.nSlices * this.nFrames; slice++) {
      imp.setSlice(slice);
      byte[] pixels = (byte[])imp.getProcessor().getPixelsCopy();
      int zoffset = this.pixelsPerSlice * slice;
      for (int i = 0; i < this.height; i++) {
        int yoffset = i * this.width;
        for (int j = 0; j < this.width; j++) {
          this.sourcePixels[zoffset + yoffset + j] = pixels[yoffset + j];
        }
      }
    }
  }

  private void initThreshold(ImagePlus imp) {
    imp.setSlice(getThresholdSampleSlice(imp));
    imp.getProcessor().setAutoThreshold(AutoThresholder.Method.Otsu, true);
    double otsuThreshold = imp.getProcessor().getMinThreshold();
    this.threshold = (int)Math.pow(2, (int)otsuThreshold / 2) / ((int)otsuThreshold / 2 - 1);
  }

  private int getThresholdSampleSlice(ImagePlus imp) {
    int maxMeanIntensitySlice = 1;
    for (int i = 2; i <= this.nSlices; i++) {
      imp.setSlice(i);
      if (imp.getRawStatistics().mean > maxMeanIntensitySlice) {
        maxMeanIntensitySlice = i;
      }
    }
    System.out.println("Sample slice: " + maxMeanIntensitySlice);
    return maxMeanIntensitySlice;
  }

  private void runInitialEstimations(ImagePlus imp) {
    this.previewSphere = sphereEstimation(voxelsOverThreshold(1));
    this.previewDistanceMap = getDistanceMap(1, previewSphere.center);
  }

  private float[] getDistanceMap(int frame, Point center) {
    float[] map = new float[this.pixelsPerFrame];
    Point currentPoint = new Point(0, 0, 0);
    for (int i = this.pixelsPerFrame * (frame - 1), j = 0; i < this.pixelsPerFrame * frame; i++, j++) {
      int indexInFrame = i % this.pixelsPerFrame;
      currentPoint.x = indexInFrame % this.width;
      currentPoint.y = (indexInFrame / this.width) % this.height;
      currentPoint.z = indexInFrame / this.pixelsPerSlice;
      map[j] = (float)realDist(currentPoint, center);
    }
    return map;
  }

  /** Ask the user for the parameters. This method of an ExtendedPlugInFilter
   *  is called by ImageJ after setup.
   * @param imp       The ImagePlus containing the image (or stack) to process.
   * @param command   The ImageJ command (as it appears the menu) that has invoked this filter
   * @param pfr       A reference to the PlugInFilterRunner, needed for preview
   * @return          Flags, i.e. a code describing supported formats etc.
   */
  public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
    System.out.println("FLAG para ShowDialog");
    GenericDialog gd = new GenericDialog("Sphere projection");
    if ((this.calibX == this.calibY) && (this.calibY == this.calibZ)) {
      gd.addMessage("ATTENTION:");
    }
    gd.addMessage("Voxel size is " + 
      Math.round(100.0 * this.calibX) / 100.0 + " [" + this.units + "] x " +
      Math.round(100.0 * this.calibY) / 100.0 + " [" + this.units + "] x " +
      Math.round(100.0 * this.calibZ) / 100.0 + " [" + this.units + "]",
      null, (this.calibX == this.calibY) && (this.calibY == this.calibZ) ? Color.RED : Color.BLACK
    );
    gd.addSlider("Radius proportion", 0.0, 1.0, 0.94);
    gd.addPreviewCheckbox(pfr);
    gd.addDialogListener(this);
    gd.showDialog();
    okPressed = gd.wasOKed();
    preview = false;
    return FLAGS;
  }

  private int voxelsOverThreshold(int frame) {
    int cnt = 0;
    for (int slice = 1; slice <= this.nSlices; slice++) {
      cnt += pixelsOverThreshold(frame, slice, cnt);
    }
    return cnt;
  }
  
  private int pixelsOverThreshold(int frame, int slice, int start) {
    int sliceOffset = (frame - 1) * this.pixelsPerFrame + (slice - 1) * this.pixelsPerSlice;
    int cnt = 0;
    for (int i = 0; i < height; i++) {
      int offset = i * width + sliceOffset;
      for (int j = 0; j < width; j++) {
        byte val = this.sourcePixels[offset + j];
        if (val > threshold || val < 0) {
          auxPointsArray[start + cnt++] = new Point(j, i, slice - 1);
        }
      }
    }
    return cnt;
  }

  /** Listener to modifications of the input fields of the dialog.
   *  Here the parameters should be read from the input dialog.
   *  @param gd The GenericDialog that the input belongs to
   *  @param e  The input event
   *  @return whether the input is valid and the filter may be run with these parameters
   */
  public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
    System.out.println("FLAG para DialogItemChanged ");
    this.proportion = (double) gd.getNextNumber();     
    return !gd.invalidNumber() && this.proportion >= 0 && this.proportion <= 1;
  }

  /**
   * This method is called by ImageJ for processing
   * @param ip The image that should be processed
   */
  public void run(ImageProcessor ip) {
    System.out.println("FLAG para RUN ");
    if (preview) {
      showPreview(this.proportion);
    }
    if (okPressed) {
      System.out.println("OK");
      processAllFrames();
    }
  }

  private void showPreview(double proportion) {
    if (this.previewCache.containsKey(new Double(proportion))) {
      IJ.getImage().setImage(new ImagePlus("Preview", this.previewCache.get(new Double(proportion))));
      return;
    }
    double admittedRadius = proportion * this.previewSphere.r;
    ImageStack previewStack = cropSphere(previewFrame, admittedRadius, this.previewDistanceMap);
    ColorProcessor icp = drawEstimations(getZProjectionProcessor(previewStack));
    if (this.previewCache.size() == 0) {
      ImagePlus impstack = new ImagePlus("Preview", icp);
      impstack.show();
    }
    else {
      IJ.getImage().setImage(new ImagePlus("Preview", icp));
    }
    this.previewCache.put(new Double(proportion), icp);
  }

  private ImageProcessor getZProjectionProcessor(ImageStack stack) {
    ZProjector projector = new ZProjector(new ImagePlus("Projection", stack));
    projector.setMethod(ZProjector.MAX_METHOD);
    projector.doProjection();
    return projector.getProjection().getProcessor();
  }
  
  private ImageStack cropSphere(int frame, double radiusToKeep, float[] frameDistanceMap) {
    ImageStack stack = new ImageStack(this.width, this.height);
    int frameOffset = (frame - 1) * this.pixelsPerFrame;
    for (int slice = 1; slice <= this.nSlices; slice++) {
      byte[] pixels = new byte[this.pixelsPerSlice];
      int offset = (slice - 1) * this.pixelsPerSlice;
      for (int j = 0; j < this.pixelsPerSlice; j++) {
        int pos = offset + j;
        pixels[j] = frameDistanceMap[pos] > radiusToKeep ? 0 : this.sourcePixels[pos + frameOffset];
      }
      stack.addSlice("Slice " + slice, pixels);
    }
    return stack;
  }
  
  private void processAllFrames() {
    ImageStack resultsStack = new ImageStack(this.width, this.height);
    ResultsTable table = new ResultsTable();
    for (int frame = 1; frame <= nFrames; frame++) {
      System.out.println("Processing frame " + frame + "...");
      int vot = voxelsOverThreshold(frame);
      Sphere sphere = sphereEstimation(vot);
      ImageStack sphereStack = cropSphere(frame, sphere.r * this.proportion, getDistanceMap(frame, sphere.center));
      resultsStack.addSlice(getZProjectionProcessor(sphereStack));
      addRowToTable(table, frame, sphere);
    }
    ImagePlus impstack = new ImagePlus("Result", resultsStack);
    impstack.show();
    table.show("Projection data");
  }

  private void addRowToTable(ResultsTable table, int frame, Sphere sphere) {
    table.incrementCounter();
    table.addValue("Frame", frame);
    table.addValue("Radius", sphere.r);
    table.addValue("CX", sphere.center.x);
    table.addValue("CY", sphere.center.y);
    table.addValue("CZ", sphere.center.z);
  }

  private ColorProcessor drawEstimations(ImageProcessor ip) {
    ColorProcessor icp = ip.convertToColorProcessor();
    drawCircle(icp, Color.RED, this.previewSphere);
    drawCircle(icp, Color.GREEN, sphereEstimation(pixelsOverThreshold(1, 1, 0)));
    drawCircle(icp, Color.ORANGE, sphereEstimation(pixelsOverThreshold(1, this.nSlices, 0)));
    return icp;
  }

  private void drawCircle(ImageProcessor ip, Color color, Sphere sphere) {
    double x = sphere.center.x, y = sphere.center.y, z = sphere.center.z, r = sphere.r;
    ip.setColor(color);
    ip.drawOval((int)(x-(r/calibX)), (int)(y-(r/calibY)), (int)((2*r)/calibX), (int)((2*r)/calibY));
  }
  
  public void setNPasses(int nPasses) {
  }
  
  private double realDist(Point p, Point q) {
    return Math.sqrt(Math.pow((p.x - q.x) * calibX, 2) + Math.pow((p.y - q.y) * calibY, 2) + Math.pow((p.z - q.z) * calibZ, 2));
  }
  
  public Sphere sphereEstimation(int nPoints) {
    Point centroid = getCentroid(nPoints);
    Point center = new Point(centroid.x, centroid.y, centroid.z);
    for (int h = 1; h <= 300; h++) {
      double Lba = 0, Lbb = 0, Lbc = 0, Lb = 0;
      for (int i = 0; i < nPoints; i++) {
        double dist = realDist(center, auxPointsArray[i]);
        Lba += (center.x - auxPointsArray[i].x) / dist;
        Lbb += (center.y - auxPointsArray[i].y) / dist;
        Lbc += (center.z - auxPointsArray[i].z) / dist;
        Lb += dist / nPoints;
      }
      center.x = centroid.x + Lb * Lba / nPoints;
      center.y = centroid.y + Lb * Lbb / nPoints;
      center.z = centroid.z + Lb * Lbc / nPoints;
    }
    double r = getAverageDistance(nPoints, center);
    return new Sphere(center, r);
  }

  private Point getCentroid(int nPoints) {
    Point sum = new Point(0, 0, 0);
    for (int j = 0; j < nPoints; j++) {
      sum.x += auxPointsArray[j].x;
      sum.y += auxPointsArray[j].y;
      sum.z += auxPointsArray[j].z;
    }
    return new Point(sum.x / nPoints, sum.y / nPoints, sum.z / nPoints);
  }

  private double getAverageDistance(int nPoints, Point p) {
    double r = 0;
    for (int i = 0; i < nPoints; i++) {
      r += realDist(p, auxPointsArray[i]);
    }
    return r / nPoints;
  }

  public static void main(String[] args) {
    new ImageJ();
    ImagePlus image = IJ.openImage(args[0]);
    IJ.runPlugIn(image, "Sphere_Projection", "parameter=value");
    WindowManager.addWindow(image.getWindow());
  }

  private class Point {
    double x, y, z;
    public Point(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }
  }

  private class Sphere {
    Point center;
    double r;
    public Sphere(Point center, double r) {
      this.center = center;
      this.r = r;
    }
  }
}
