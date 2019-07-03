import ij.*;
import ij.process.*;
import ij.plugin.ZProjector;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;

import java.awt.*;

public class Proyeccion_Esfera implements ExtendedPlugInFilter, DialogListener {

  private static int FLAGS =      //bitwise or of the following flags:
          STACK_REQUIRED |
          DOES_ALL |              //this plugin processes 8-bit, 16-bit, 32-bit gray & 24-bit/pxl RGB
          KEEP_PREVIEW;           //When using preview, the preview image can be kept as a result

  private ImageStack source;
  private byte[] sourcePixels;
  private float[] distanceMap;
  private ImagePlus sourceImagePlus;
  private double pelar;
  private boolean preview = false;
  private boolean okPressed = false;
  
  private double x_z0_pre, y_z0_pre, x_zs_pre, y_zs_pre, r_z0_pre, r_zs_pre;
  private double xpre, ypre, zpre, rpre;
  private double factorx, factory, factorz;
  private int width, height, zs, tiempos, threshold, pixelsPerSlice;
  
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
    this.preview = true;
    this.sourceImagePlus = imp;
    this.source = imp.getStack();
    this.tiempos = imp.getNFrames(); //tiempos a procesar
    this.width = source.getWidth(); //ancho de imagen
    this.height = source.getHeight(); //alto de imagen
    this.pixelsPerSlice = this.width * this.height;
    this.zs = source.getSize() / this.tiempos; // numero de planos z

    this.sourcePixels = new byte[this.width * this.height * this.zs * this.tiempos];
    for (int z = 1; z <= zs; z++) {
      this.sourceImagePlus.setSlice(z);
      byte[] pixels = (byte[])this.sourceImagePlus.getProcessor().getPixelsCopy();
      int zoffset = this.width * this.height * (z - 1);
      for (int i = 0; i < height; i++) {
        int yoffset = i * width;
        for (int j = 0; j < width; j++) {
          this.sourcePixels[zoffset + yoffset + j] = pixels[yoffset + j];
        }
      }
    }

    this.sourceImagePlus.setSlice(1);
    this.sourceImagePlus.getProcessor().setAutoThreshold(AutoThresholder.Method.Otsu, true);
    double otsuThreshold = imp.getProcessor().getMinThreshold();
    this.threshold = (int)Math.pow(2, (int)otsuThreshold / 2) / ((int)otsuThreshold / 2 - 1);

    runInitialEstimations(imp);
    return FLAGS;
  }

  private void runInitialEstimations(ImagePlus imp) {
    // arreglos para guardar datos para estimacion de la esfera
    int dimension = width * height * zs;
    double[] mx = new double[dimension];
    double[] my = new double[dimension];
    double[] mz = new double[dimension];
    
    // circulo para z=0
    int cnt = pixelsOverThreshold(1, 0, mx, my, mz);
    double[] datos = circleEstimation(mx, my, cnt);
    x_z0_pre = datos[0];
    y_z0_pre = datos[1];
    r_z0_pre = datos[2];

    // circulo para z=zs
    cnt = pixelsOverThreshold(zs, 0, mx, my, mz);
    datos = circleEstimation(mx, my, cnt);
    x_zs_pre = datos[0];
    y_zs_pre = datos[1];
    r_zs_pre = datos[2];
    
    // estimar esfera
    cnt = 0;
    for (int z = 1; z <= zs; z++) {
      cnt += pixelsOverThreshold(z, cnt, mx, my, mz);
    }
    datos = sphereEstimation(mx, my, mz, cnt);
    xpre = datos[0];
    ypre = datos[1];
    zpre = datos[2];
    rpre = datos[3];

    this.distanceMap = new float[this.sourcePixels.length];
    for (int i = 0; i < this.sourcePixels.length; i++) {
      int cx = i % this.width;
      int cy = (i / this.width) % this.height;
      int cz = i / this.pixelsPerSlice;
      this.distanceMap[i] = (float)realDist(cx - xpre, cy - ypre, cz - zpre);
    }
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
    GenericDialog gd = new GenericDialog("Factor de corte");
    gd.addSlider("Ingrese un numero entre 0 y 1", 0.0, 1.0, 0.94);
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
  
  private int pixelsOverThreshold(int slice, int offset, double[] x, double[] y, double[] z) {
    int cnt = 0;
    for (int i = 0; i < height; i++) {
      int offsetaux = i * width;
      for (int j = 0; j < width; j++) {
        int pos = offsetaux + j;
        if (this.sourcePixels[(slice - 1) * width * height + pos] > threshold || this.sourcePixels[(slice - 1) * width * height + pos] < 0) {
          x[offset + cnt] = j;
          y[offset + cnt] = i;
          z[offset + cnt] = slice - 1;
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
    pelar = (double) gd.getNextNumber();     
    return !gd.invalidNumber() && pelar >= 0 && pelar <= 1;
  }

  /**
   * This method is called by ImageJ for processing
   * @param ip The image that should be processed
   */
  public void run(ImageProcessor ip) {
    System.out.println("FLAG para RUN ");
    
    if (preview) {
      System.out.println("Se calculara con este valor de pelado" + pelar);
      System.out.println("Paso 1");
      ImageStack stack_prev = new ImageStack(width, height, zs);

      System.out.println("Paso 2");

      double radioAdmitido = pelar * rpre;
      for (int i = 0; i < this.sourcePixels.length; i += this.pixelsPerSlice) {
        byte[] slice = new byte[this.pixelsPerSlice];
        for (int j = 0; j < this.pixelsPerSlice; j++) {
          int pos = i + j;
          slice[j] = distanceMap[pos] > radioAdmitido ? 0 : this.sourcePixels[pos];
        }
        stack_prev.setPixels(slice, 1 + i / this.pixelsPerSlice);
      }
      System.out.println("Valor pelar * rpre :" + pelar * rpre);
      
      System.out.println("Paso 3");
      
      ImagePlus histIm = new ImagePlus("Proyección", stack_prev); 
      ZProjector projector = new ZProjector(histIm);
      projector.setMethod(ZProjector.MAX_METHOD);
      projector.doProjection();
      
      //convertir a color para dibujar circunferencias
      ImageProcessor proyeccion = projector.getProjection().getProcessor();
      ColorProcessor ipc = proyeccion.convertToColorProcessor();
      
      //dibujar circulo de la aproximacion completa
      ipc.setColor(Color.RED);
      ipc.drawOval((int)(xpre-(rpre/factorx)), (int)(ypre-(rpre/factory)), (int)((2*rpre)/factorx), (int)((2*rpre)/factory));
          
      //circulo primer slice
      if (r_z0_pre > 20) {
        ipc.setColor(Color.GREEN);
        ipc.drawOval((int)(x_z0_pre-(r_z0_pre/factorx)), (int)(y_z0_pre-(r_z0_pre/factory)), (int)((2*r_z0_pre)/factorx), (int)((2*r_z0_pre)/factory));
      }
      
      //circulo ultimo slice
      if (r_zs_pre > 20) { 
        ipc.setColor(Color.ORANGE);
        ipc.drawOval((int)(x_zs_pre-(r_zs_pre/factorx)), (int)(y_zs_pre-(r_zs_pre/factory)), (int)((2*r_zs_pre)/factorx), (int)((2*r_zs_pre)/factory));
      }

      ImagePlus impstack = new ImagePlus("MAX_stack_hermoso", ipc);
      impstack.show();
    }
    if (!preview && !okPressed) {
      System.out.println("Si calculo todo sin el preview");
    }
    if (!preview && okPressed) {
      System.out.println("Si calculo todo sin el preview y con ok");
      
      //duplicar para no modificar stack original
      ImagePlus img2 = sourceImagePlus.duplicate();
      
      ImageStack stack = img2.getStack();
      
      byte[] pixels;  //arreglo para guardar slices del stack
      byte[] pix_est; //para estimar el centro y radio de la esfera
      
      int x, offset, offsetaux, pos;  //para buscar coordenadas en el stack 
      double distancia;
      
      //stack auxiliar para guardar stack proyectado
      ImageStack stack_proyeccion = new ImageStack(width, height);    
              
      //centro y radio de la esfera
      double[] x0 = new double[tiempos];
      double[] y0 = new double[tiempos];
      double[] z0 = new double[tiempos];
      double[] r = new double[tiempos];
                
      //dimension de arreglos para guardar datos
      int dimension = (int) width*height*zs/15;
        
      //arreglos para guardar datos para estimacion de la esfera
      double[] mx = new double[dimension];
      double[] my = new double[dimension];
      double[] mz = new double[dimension];
        
      double[] datos;
      int cnt = 0;
        
      ZProjector projector = new ZProjector(img2); 
      ImageProcessor proyeccion;
      
      projector.setMethod(ZProjector.MAX_METHOD);
        
      double rmax = 0;
      ColorProcessor ipc;
      double x_z0, y_z0, x_zs, y_zs, r_z0, r_zs;

      //circulo para z=0
      pix_est = (byte[]) stack.getPixels(1);
      for (int i=0; i < height; i++) {
          offsetaux = i*width;
          for (int j=0; j < width; j++) {
            pos = offsetaux + j;
            if ((pix_est[pos]) > threshold || pix_est[pos] < 0) {
              mx[cnt] = j;
              my[cnt] = i;
              cnt += 1;
            }
          }
      }
        
      datos = circleEstimation(mx, my, cnt);
      x_z0 = datos[0];
      y_z0 = datos[1];
      r_z0 = datos[2];
      cnt = 0;
      
      //circulo para z=zs
      pix_est = (byte[]) stack.getPixels(zs);
      for (int i=0; i < height; i++) {
        offsetaux = i*width;
        for (int j=0; j < width; j++) {
          pos = offsetaux + j;
          if ((pix_est[pos]) > threshold || pix_est[pos] < 0) {
              mx[cnt] = j;
              my[cnt] = i;
              cnt += 1;
          }
        }
      }
        
      datos = circleEstimation(mx, my, cnt);
      x_zs = datos[0];
      y_zs = datos[1];
      r_zs = datos[2];
      cnt = 0;
      double xp = 0, yp = 0;        
        
      //proyectar para cada t
      for(int t=1; t <= tiempos; t++) {
        
        //estimar esfera
        for(int h=1; h <= zs; h++) {
          pix_est = (byte[]) stack.getPixels(h+(t-1)*zs);
          for (int i=0; i < height; i++) {
            offsetaux = i*width;
            for (int j=0; j < width; j++) {
              pos = offsetaux + j;
              if ((pix_est[pos]) > threshold || pix_est[pos] < 0) {
                mx[cnt] = j;
                my[cnt] = i;
                mz[cnt] = h-1;
                cnt += 1;
              }
            }
          }
        }
          
        datos = sphereEstimation(mx, my, mz, cnt);
          
        x0[t-1] = datos[0];
        y0[t-1] = datos[1];
        z0[t-1] = datos[2];
        r[t-1] = datos[3];
        cnt = 0;
        
        byte[] pixel_aux;
        byte[] pixel_aux2;
          
        pixel_aux = (byte[]) stack.getPixels(zs);
                      
        for (int h = 1; h <= zs; h++) {
          pixels = (byte[]) stack.getPixels(h+(t-1)*zs);
          for(int i = 0; i < height; i++) {
            offset = i*width;
            for(int j=0; j < width ; j++) {
              distancia = Math.sqrt(Math.pow(((j-x0[t-1])*factorx),2) + Math.pow(((i-y0[t-1])*factory),2)
              + Math.pow(((h-z0[t-1])*factorz),2));
              x = offset + j;
              if (distancia > pelar*r[t-1]) {
                pixels[x] = 0;
              }
            }
          }
        }
        pixel_aux2 = (byte[]) stack.getPixels(zs);
        System.out.println("Pixel 100000 vale :"+ pixel_aux2[100000]);
        
        // proyectar sobre todo z para el tiempo t
        projector.setStartSlice(t + (t-1)*zs);
        projector.setStopSlice(t*zs);
        projector.doProjection();
          
        proyeccion = projector.getProjection().getProcessor();
          
        //convertir a color para dibujar circunferencias
        ipc = proyeccion.convertToColorProcessor();
          
        //dibujar circulo de la aproximacion completa
        ipc.setColor(Color.RED);
        ipc.drawOval((int)(x0[t-1]-(r[t-1]/factorx)), (int)(y0[t-1]-(r[t-1]/factory)), (int)((2*r[t-1])/factorx), (int)((2*r[t-1])/factory));
          
        //circulo primer slice
        if (r_z0 > 20) {
          ipc.setColor(Color.GREEN);
          ipc.drawOval((int)(x_z0-(r_z0/factorx)), (int)(y_z0-(r_z0/factory)), (int)((2*r_z0)/factorx), (int)((2*r_z0)/factory));
        }
          
        //circulo ultimo slice
        if (r_zs > 20) { 
          ipc.setColor(Color.ORANGE);
          ipc.drawOval((int)(x_zs-(r_zs/factorx)), (int)(y_zs-(r_zs/factory)), (int)((2*r_zs)/factorx), (int)((2*r_zs)/factory));
        }
          
        //agregar slice con todos los circulos dibujadoss
        stack_proyeccion.addSlice(ipc);
      }
      ImagePlus impstack = new ImagePlus("MAX_stack_hermoso", stack_proyeccion);
      impstack.show();  
    } 
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
