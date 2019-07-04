import ij.*;
import ij.process.*;
import ij.plugin.ZProjector;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import java.util.HashMap;

import java.awt.*;

public class Proyeccion_Esfera implements ExtendedPlugInFilter, DialogListener {

  private static int FLAGS =      //bitwise or of the following flags:
          STACK_REQUIRED |
          DOES_ALL |              //this plugin processes 8-bit, 16-bit, 32-bit gray & 24-bit/pxl RGB
          KEEP_PREVIEW;           //When using preview, the preview image can be kept as a result

  private byte[] sourcePixels;
  private float[] distanceMap;
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
    double[] datos = circleEstimation(mx, my, cnt);
    x_z0_pre = datos[0];
    y_z0_pre = datos[1];
    r_z0_pre = datos[2];

    // circulo para z=zs
    cnt = pixelsOverThreshold(1, this.nSlices, 0, mx, my, mz);
    datos = circleEstimation(mx, my, cnt);
    x_zs_pre = datos[0];
    y_zs_pre = datos[1];
    r_zs_pre = datos[2];
    
    // estimar esfera
    cnt = 0;
    for (int slice = 1; slice <= this.nSlices; slice++) {
      cnt += pixelsOverThreshold(1, slice, cnt, mx, my, mz);
    }
    datos = sphereEstimation(mx, my, mz, cnt);
    xpre = datos[0];
    ypre = datos[1];
    zpre = datos[2];
    rpre = datos[3];

    this.distanceMap = getDistanceMap(1, xpre, ypre, zpre);
  }

  private float[] getDistanceMap(int frame, double x, double y, double z) {
    float[] map = new float[this.pixelsPerFrame];
    int from = this.pixelsPerFrame * (frame - 1);
    int to = this.pixelsPerFrame * frame;
    for (int i = from; i < to; i++) {
      int cx = i % this.width;
      int cy = (i / this.width) % this.height;
      int cz = i / this.pixelsPerSlice;
      map[i] = (float)realDist(cx - x, cy - y, cz - z);
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
    ImageStack previewStack = cropSphere(previewTime, admittedRadius);
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

  private ImageStack cropSphere(int frame, double radius) {
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
      double x, y, z, r;
      
      //estimar esfera
      int cnt = 0;
      for (int slice = 1; slice <= this.nSlices; slice++) {
        cnt += pixelsOverThreshold(frame, slice, cnt, mx, my, mz);
      }
      double[] datos = sphereEstimation(mx, my, mz, cnt);
      
      x = datos[0];
      y = datos[1];
      z = datos[2];
      r = datos[3];
      cnt = 0;
      
      ImageStack sphere = cropSphere(frame, r * proportion);
      
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
  public void setNPasses (int nPasses) {
    System.out.println("FLAG para setNpasses");
  }
  
  private double realDist(double dx, double dy, double dz) {
    return Math.sqrt(Math.pow(dx * factorx, 2) + Math.pow(dy * factory, 2) + Math.pow(dz * factorz, 2));
  }
  
  private double realDist(double dx, double dy) {
    return realDist(dx, dy, 0);
  }
  
  // El metodo asume que los puntos no son todos coplanares
  // y que estan aproximadamente en el borde
  public double[] sphereEstimation(double[] x, double[] y, double[] z, int nPoints) {
    double[] centroid = getCentroid(x, y, z, nPoints);
    double a = centroid[0];
    double b = centroid[1];
    double c = centroid[2];    
    for (int h = 1; h <= 300; h++) {
      double Lba = 0, Lbb = 0, Lbc = 0, Lb = 0;
      for (int i = 0; i < nPoints; i++) {
          double dist = realDist(x[i] - a, y[i] - b, z[i] - c);
          Lba += (a - x[i]) / (dist * nPoints);
          Lbb += (b - y[i]) / (dist * nPoints);
          Lbc += (c - z[i]) / (dist * nPoints);
          Lb += dist / nPoints;
      }
      a = centroid[0] + Lb * Lba;
      b = centroid[1] + Lb * Lbb;
      c = centroid[2] + Lb * Lbc;
    }
    double r = getAverageDistance(x, y, z, nPoints, a, b, c);
    return new double[] {a, b, c, r};
  }

  private double[] getCentroid(double[] x, double[] y, double[] z, int nPoints) {
    double avgX = 0, avgY = 0, avgZ = 0;
    for (int j = 0; j < nPoints; j++) {
      avgX += x[j];
      avgY += y[j];
      avgZ += z[j];
    }
    avgX /= nPoints;
    avgY /= nPoints;
    avgZ /= nPoints;
    return new double[] {avgX, avgY, avgZ};
  }

  private double getAverageDistance(double[] x, double[] y, double[] z, int nPoints, double a, double b, double c) {
    double r = 0;
    for (int i = 0; i < nPoints; i++) {
      r += realDist(x[i] - a, y[i] - b, z[i] - c);
    }
    return r / nPoints;
  }
  
  public double[] circleEstimation(double[] x, double[] y, int nPoints) {
    double[] centroid = getCentroid(x, y, nPoints);
    double a = centroid[0];
    double b = centroid[1];
    for (int iter = 0; iter < 300; iter++) {
      double Lba = 0, Lbb = 0, Lb = 0, dist = 0;
      for (int i = 0; i < nPoints; i++) {
        dist = realDist(x[i] - a, y[i] - b);
        Lba += (a - x[i]) / (dist * nPoints);
        Lbb += (b - y[i]) / (dist * nPoints);
        Lb += dist / nPoints;
      }
      a = centroid[0] + Lb * Lba;
      b = centroid[1] + Lb * Lbb;
    }
    double r = getAverageDistance(x, y, nPoints, a, b);
    return new double[] {a, b, r};
  }

  private double[] getCentroid(double[] x, double[] y, int nPoints) {
    double avgX = 0, avgY = 0;
    for (int j = 0; j < nPoints; j++) {
      avgX += x[j];
      avgY += y[j];
    }
    avgX /= nPoints;
    avgY /= nPoints;
    return new double[] {avgX, avgY};
  }

  private double getAverageDistance(double[] x, double[] y, int nPoints, double a, double b) {
    double r = 0;
    for (int i = 0; i < nPoints; i++) {
      r += realDist(x[i] - a, y[i] - b);
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
}
