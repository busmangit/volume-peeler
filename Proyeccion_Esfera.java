import ij.*;
import ij.process.*;
import ij.plugin.ZProjector;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import java.util.HashMap;
import java.awt.Color;
import java.awt.AWTEvent;

public class Proyeccion_Esfera implements ExtendedPlugInFilter, DialogListener {

  private static int FLAGS = STACK_REQUIRED | DOES_ALL;
  private byte[] sourcePixels;
  private float[] previewDistanceMap;
  private ImagePlus sourceImagePlus;
  private double proportion;
  private boolean preview = true;
  private boolean okPressed = false;
  private HashMap<Double, ColorProcessor> previewCache;
  
  private double x_z0_pre, y_z0_pre, x_zs_pre, y_zs_pre, r_z0_pre, r_zs_pre;
  private double xpre, ypre, zpre, rpre;
  private double factorx, factory, factorz;
  private int width, height, nSlices, nFrames, threshold, pixelsPerSlice, pixelsPerFrame;
  
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
    this.factorx = cal.pixelWidth; //x contains the pixel width in units
    this.factory = cal.pixelHeight; //y contains the pixel height in units
    this.factorz = cal.pixelDepth; //z contains the pixel (voxel) depth in units
    this.sourceImagePlus = imp;
    this.width = imp.getWidth(); //ancho de imagen
    this.height = imp.getHeight(); //alto de imagen
    this.nFrames = imp.getNFrames(); //nFrames a procesar
    this.nSlices = imp.getNSlices() / this.nFrames; // numero de planos z
    this.pixelsPerSlice = this.width * this.height;
    this.pixelsPerFrame = this.pixelsPerSlice * this.nSlices;
    this.previewCache = new HashMap<Double, ColorProcessor>();
    initSourcePixels();
    initThreshold();
    runInitialEstimations(imp);
    return FLAGS;
  }

  private void initSourcePixels() {
    this.sourcePixels = new byte[this.pixelsPerSlice * this.nSlices * this.nFrames];
    for (int slice = 1; slice <= this.nSlices; slice++) {
      this.sourceImagePlus.setSlice(slice);
      byte[] pixels = (byte[])this.sourceImagePlus.getProcessor().getPixelsCopy();
      int zoffset = this.pixelsPerSlice * (slice - 1);
      for (int i = 0; i < this.height; i++) {
        int yoffset = i * this.width;
        for (int j = 0; j < this.width; j++) {
          this.sourcePixels[zoffset + yoffset + j] = pixels[yoffset + j];
        }
      }
    }
  }

  private void initThreshold() {
    int thresholdSampleSlice = this.nSlices / 2;
    this.sourceImagePlus.setSlice(thresholdSampleSlice);
    this.sourceImagePlus.getProcessor().setAutoThreshold(AutoThresholder.Method.Otsu, true);
    double otsuThreshold = this.sourceImagePlus.getProcessor().getMinThreshold();
    this.threshold = (int)Math.pow(2, (int)otsuThreshold / 2) / ((int)otsuThreshold / 2 - 1);
  }

  private void runInitialEstimations(ImagePlus imp) {
    // arreglos para guardar datos para estimacion de la esfera
    int dimension = this.pixelsPerFrame / 15;
    double[] mx = new double[dimension];
    double[] my = new double[dimension];
    double[] mz = new double[dimension];
    
    // circulo para z=0
    int cnt = pixelsOverThreshold(1, 1, 0, mx, my, mz);
    Circle est = circleEstimation(mx, my, cnt);
    x_z0_pre = est.center.x;
    y_z0_pre = est.center.y;
    r_z0_pre = est.r;

    // circulo para z=zs
    cnt = pixelsOverThreshold(1, this.nSlices, 0, mx, my, mz);
    est = circleEstimation(mx, my, cnt);
    x_zs_pre = est.center.x;
    y_zs_pre = est.center.y;
    r_zs_pre = est.r;
    
    // estimar esfera
    cnt = 0;
    for (int slice = 1; slice <= this.nSlices; slice++) {
      cnt += pixelsOverThreshold(1, slice, cnt, mx, my, mz);
    }
    est = sphereEstimation(mx, my, mz, cnt);
    xpre = est.center.x;
    ypre = est.center.y;
    zpre = est.center.z;
    rpre = est.r;

    this.previewDistanceMap = getDistanceMap(1, est.center);
  }

  private float[] getDistanceMap(int frame, Point center) {
    float[] map = new float[this.pixelsPerFrame];
    int from = this.pixelsPerFrame * (frame - 1);
    int to = this.pixelsPerFrame * frame;
    for (int i = from; i < to; i++) {
      int x = i % this.width;
      int y = (i / this.width) % this.height;
      int z = i / this.pixelsPerSlice;
      map[i] = (float)realDist(new Point(x, y, z), center);
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
    gd.addSlider("Radius proportion", 0.0, 1.0, 0.94);
    gd.addPreviewCheckbox(pfr);
    gd.addDialogListener(this);
    gd.showDialog();
    if (gd.wasCanceled()) {
      return DONE;
    }
    okPressed = gd.wasOKed();
    preview = false;
    return FLAGS;
  }
  
  private int pixelsOverThreshold(int frame, int slice, int prevCnt, double[] x, double[] y, double[] z) {
    int frameOffset = (frame - 1) * this.pixelsPerFrame;
    int cnt = 0;
    for (int i = 0; i < height; i++) {
      int offsetaux = i * width + frameOffset;
      for (int j = 0; j < width; j++) {
        int pos = offsetaux + j;
        if (this.sourcePixels[(slice - 1) * this.pixelsPerSlice + pos] > threshold || this.sourcePixels[(slice - 1) * this.pixelsPerSlice + pos] < 0) {
          x[prevCnt + cnt] = j;
          y[prevCnt + cnt] = i;
          z[prevCnt + cnt] = slice - 1;
          cnt++;
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
      processAllFrames();
    } 
  }

  private void showPreview(double proportion) {
    if (this.previewCache.containsKey(new Double(proportion))) {
      IJ.getImage().setImage(new ImagePlus("Preview", this.previewCache.get(new Double(proportion))));
      return;
    }
    int previewTime = 1;
    double admittedRadius = proportion * rpre;
    ImageStack previewStack = cropSphere(previewTime, admittedRadius, this.previewDistanceMap);
    ZProjector projector = new ZProjector(new ImagePlus("Projection", previewStack));
    projector.setMethod(ZProjector.MAX_METHOD);
    projector.doProjection();
    ColorProcessor icp = drawEstimations(projector.getProjection().getProcessor());
    if (this.previewCache.size() == 0) {
      ImagePlus impstack = new ImagePlus("Preview", icp);
      impstack.show();
    }
    else {
      IJ.getImage().setImage(new ImagePlus("Preview", icp));
    }
    this.previewCache.put(new Double(proportion), icp);
  }

  private ImageStack cropSphere(int frame, double radius, float[] distanceMap) {
    ImageStack stack = new ImageStack(this.width, this.height, this.nSlices);
    int frameOffset = (frame - 1) * this.pixelsPerFrame;
    for (int i = 0; i < this.pixelsPerSlice * this.nSlices; i += this.pixelsPerSlice) {
      byte[] slice = new byte[this.pixelsPerSlice];
      for (int j = 0; j < this.pixelsPerSlice; j++) {
        int pos = i + j + frameOffset;
        slice[j] = distanceMap[pos] > radius ? 0 : this.sourcePixels[pos];
      }
      stack.setPixels(slice, 1 + i / this.pixelsPerSlice);
    }
    return stack;
  }

  private void processAllFrames() {
    System.out.println("OK");
    
    ImageStack resultsStack = new ImageStack(this.width, this.height, this.nSlices);
      
    //arreglos para guardar datos para estimacion de la esfera
    int dimension = this.pixelsPerFrame / 15;
    double[] mx = new double[dimension];
    double[] my = new double[dimension];
    double[] mz = new double[dimension];
    
    //proyectar para cada frame
    for(int frame = 1; frame <= nFrames; frame++) {
      
      //estimar esfera
      double x, y, z, r;
      int cnt = 0;
      for (int slice = 1; slice <= this.nSlices; slice++) {
        cnt += pixelsOverThreshold(frame, slice, cnt, mx, my, mz);
      }
      Circle est = sphereEstimation(mx, my, mz, cnt);
      ImageStack sphere = cropSphere(frame, est.r * proportion, getDistanceMap(frame, est.center));
      
      // proyectar sobre todo z para el tiempo t
      ZProjector projector = new ZProjector(new ImagePlus("Projection", resultsStack));
      projector.setMethod(ZProjector.MAX_METHOD);
      projector.doProjection();  
      ColorProcessor ipc = drawEstimations(projector.getProjection().getProcessor());
      resultsStack.addSlice(ipc);
    }
    ImagePlus impstack = new ImagePlus("Result", resultsStack);
    impstack.show();
  }

  private ColorProcessor drawEstimations(ImageProcessor ip) {
    ColorProcessor icp = ip.convertToColorProcessor();
          
    //dibujar circulo de la aproximacion completa
    icp.setColor(Color.RED);
    icp.drawOval((int)(xpre-(rpre/factorx)), (int)(ypre-(rpre/factory)), (int)((2*rpre)/factorx), (int)((2*rpre)/factory));

    //circulo primer slice
    if (r_z0_pre > 20) {
      icp.setColor(Color.GREEN);
      icp.drawOval((int)(x_z0_pre-(r_z0_pre/factorx)), (int)(y_z0_pre-(r_z0_pre/factory)), (int)((2*r_z0_pre)/factorx), (int)((2*r_z0_pre)/factory));
    }

    //circulo ultimo slice
    if (r_zs_pre > 20) { 
      icp.setColor(Color.ORANGE);
      icp.drawOval((int)(x_zs_pre-(r_zs_pre/factorx)), (int)(y_zs_pre-(r_zs_pre/factory)), (int)((2*r_zs_pre)/factorx), (int)((2*r_zs_pre)/factory));
    }
    return icp;
  }

  /** And here you do the actual cross-fade */
  /** Set the number of calls of the run(ip) method. This information is
  *  needed for displaying a progress bar; unused here.
  */
  public void setNPasses(int nPasses) {
    System.out.println("FLAG para setNpasses");
  }
  
  private double realDist(Point p, Point q) {
    return Math.sqrt(Math.pow((p.x - q.x) * factorx, 2) + Math.pow((p.y - q.y) * factory, 2) + Math.pow((p.z - q.z) * factorz, 2));
  }
  
  // El metodo asume que los puntos no son todos coplanares
  // y que estan aproximadamente en el borde
  public Circle sphereEstimation(double[] x, double[] y, double[] z, int nPoints) {
    Point centroid = getCentroid(x, y, z, nPoints);
    Point center = new Point(centroid.x, centroid.y, centroid.z);
    for (int h = 1; h <= 300; h++) {
      double Lba = 0, Lbb = 0, Lbc = 0, Lb = 0;
      for (int i = 0; i < nPoints; i++) {
          double dist = realDist(new Point(x[i], y[i], z[i]), center);
          Lba += (center.x - x[i]) / (dist * nPoints);
          Lbb += (center.y - y[i]) / (dist * nPoints);
          Lbc += (center.z - z[i]) / (dist * nPoints);
          Lb += dist / nPoints;
      }
      center.x = centroid.x + Lb * Lba;
      center.y = centroid.y + Lb * Lbb;
      center.z = centroid.z + Lb * Lbc;
    }
    double r = getAverageDistance(x, y, z, nPoints, center.x, center.y, center.z);
    return new Circle(center, r, 3);
  }

  private Point getCentroid(double[] x, double[] y, double[] z, int nPoints) {
    double avgX = 0, avgY = 0, avgZ = 0;
    for (int j = 0; j < nPoints; j++) {
      avgX += x[j];
      avgY += y[j];
      avgZ += z[j];
    }
    avgX /= nPoints;
    avgY /= nPoints;
    avgZ /= nPoints;
    return new Point(avgX, avgY, avgZ);
  }

  private double getAverageDistance(double[] x, double[] y, double[] z, int nPoints, double a, double b, double c) {
    double r = 0;
    for (int i = 0; i < nPoints; i++) {
      r += realDist(new Point(x[i], y[i], z[i]), new Point(a, b, c));
    }
    return r / nPoints;
  }
  
  public Circle circleEstimation(double[] x, double[] y, int nPoints) {
    Point centroid = getCentroid(x, y, nPoints);
    Point center = new Point(centroid.x, centroid.y);
    for (int iter = 0; iter < 300; iter++) {
      double Lba = 0, Lbb = 0, Lb = 0, dist = 0;
      for (int i = 0; i < nPoints; i++) {
        dist = realDist(new Point(x[i], y[i]), center);
        Lba += (center.x - x[i]) / (dist * nPoints);
        Lbb += (center.y - y[i]) / (dist * nPoints);
        Lb += dist / nPoints;
      }
      center.x = centroid.x + Lb * Lba;
      center.y = centroid.y + Lb * Lbb;
    }
    double r = getAverageDistance(x, y, nPoints, center.x, center.y);
    return new Circle(center, r, 2);
  }

  private Point getCentroid(double[] x, double[] y, int nPoints) {
    double avgX = 0, avgY = 0;
    for (int j = 0; j < nPoints; j++) {
      avgX += x[j];
      avgY += y[j];
    }
    avgX /= nPoints;
    avgY /= nPoints;
    return new Point(avgX, avgY);
  }

  private double getAverageDistance(double[] x, double[] y, int nPoints, double a, double b) {
    double r = 0;
    for (int i = 0; i < nPoints; i++) {
      r += realDist(new Point(x[i], y[i]), new Point(a, b));
    }
    return r / nPoints;
  }

  public static void main(String[] args) {
    new ImageJ();
    ImagePlus image = IJ.openImage(args[0]);
    IJ.runPlugIn(image, "Proyeccion_Esfera", "parameter=value");
    // image.show();
    WindowManager.addWindow(image.getWindow());
  }

  private class Point {
    double x, y, z;
    public Point(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }
    public Point(double x, double y) {
      this.x = x;
      this.y = y;
    }
  }

  private class Circle {
    Point center;
    double r;
    int dimensions;
    public Circle(Point center, double r, int d) {
      this.center = center;
      this.r = r;
      this.dimensions = d;
    }
  }
}
