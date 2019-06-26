import ij.*;
import ij.process.*;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;

import java.util.Vector;

import java.awt.*;

public class Proyeccion_Esfera implements ExtendedPlugInFilter, DialogListener {

  private static int FLAGS =      //bitwise or of the following flags:
          STACK_REQUIRED |
          DOES_ALL |              //this plugin processes 8-bit, 16-bit, 32-bit gray & 24-bit/pxl RGB
          KEEP_PREVIEW;           //When using preview, the preview image can be kept as a result

  private double percentage;      //how much of the other slice is shown
  private ImageProcessor otherSlice;  //Image data of the other slice
  protected ImageStack stack;
  static double factor;
  static double pelar;
  private boolean preview = false;
  private boolean kkk = false;
  private ImagePlus imp;
  
  private double x_z0_pre, y_z0_pre, x_zs_pre, y_zs_pre, r_z0_pre, r_zs_pre;
  
  private double xpre;
  private double ypre;
  private double zpre;
  private double rpre;

  private int width;
  private int height;
  private int zs;

  private double factorx;
  private double factory;
  private double factorz;

  private String direc;
  private String imagedir;  
  
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
  public int setup (String arg, ImagePlus imp) {
      return FLAGS;
  }

    /** Ask the user for the parameters. This method of an ExtendedPlugInFilter
     *  is called by ImageJ after setup.
     * @param imp       The ImagePlus containing the image (or stack) to process.
     * @param command   The ImageJ command (as it appears the menu) that has invoked this filter
     * @param pfr       A reference to the PlugInFilterRunner, needed for preview
     * @return          Flags, i.e. a code describing supported formats etc.
     */
  public int showDialog (ImagePlus imp, String command, PlugInFilterRunner pfr) {
        
    System.out.println("FLAG para ShowDialog ");
    preview = true;
    imp = WindowManager.getCurrentImage(); 
    ImagePlus img2 = imp;
      
    stack = img2.getStack();    
    direc = imp.getOriginalFileInfo().directory;
    imagedir = imp.getOriginalFileInfo().fileName;
    
    System.out.println("Directorio de la imagen"+ direc);
    System.out.println("Directorio de la imagen"+ imagedir);
    
    int tiempos = img2.getNFrames(); //tiempos a procesar
    
    zs = stack.getSize()/tiempos; // numero de planos z
    width = stack.getWidth(); //ancho de imagen
    height = stack.getHeight(); //alto de imagen
    
    //  byte[] pixels;  //arreglo para guardar slices del stack
    byte[] pix_est; //para estimar el centro y radio de la esfera
    
    int x, offset, offsetaux, pos;  //para buscar coordenadas en el stack 
    double distancia;
    
    //stack auxiliar para guardar stack proyectado
    ImageStack stack_proyeccion = new ImageStack(width, height);    
    
    //IJ.showMessage("Numero de tiempos:  " + tiempos + " \nNumero de planos:  " + zs);
    
    //centro y radio de la esfera
    double[] x0 = new double[tiempos];
    double[] y0 = new double[tiempos];
    double[] z0 = new double[tiempos];
    double[] r = new double[tiempos];
    
    //conversion de voxeles a micrones
    Calibration cal = imp.getCalibration(); 
        double factorx = cal.pixelWidth; //x contains the pixel width in units 
        double factory = cal.pixelHeight; //y contains the pixel height in units 
        double factorz = cal.pixelDepth; //z contains the pixel (voxel) depth in units 

    img2.getProcessor().setAutoThreshold(AutoThresholder.Method.Otsu, true);
    
    double threshold = img2.getProcessor().getMinThreshold();
    
    double factor_otsu = Math.pow(2, (int) threshold/2)/((int) threshold/2 - 1);
    
    //dimension de arreglos para guardar datos
    int dimension = (int) width*height*zs/15;
    
    //arreglos para guardar datos para estimacion de la esfera
    double[] mx = new double[dimension];
    double[] my = new double[dimension];
    double[] mz = new double[dimension];
    
    double[] datos;
    int cnt = 0;

    double rmax = 0;
    double x_z0, y_z0, x_zs, y_zs, r_z0, r_zs;

    //circulo para z=0
    pix_est = (byte[]) stack.getPixels(1);
    for (int i=0; i < height; i++) {
      offsetaux = i*width;
      for (int j=0; j < width; j++) {
        pos = offsetaux + j;
        if ((pix_est[pos]) > (int) factor_otsu || pix_est[pos] < 0) {
          mx[cnt] = j;
          my[cnt] = i;
          cnt += 1;
        }
      }
    }
      
    datos = circle_estimation(mx,my,cnt, imp);
      
    x_z0_pre = datos[0];
    y_z0_pre = datos[1];
    r_z0_pre = datos[2];
    cnt = 0;
    //circulo para z=zs
    pix_est = (byte[]) stack.getPixels(zs);
    for (int i=0; i < height; i++) {
      offsetaux = i*width;
      for (int j=0; j < width; j++) {
        pos = offsetaux + j;
        if ((pix_est[pos]) > (int) factor_otsu || pix_est[pos] < 0) {
          mx[cnt] = j;
          my[cnt] = i;
          cnt += 1;
        }
      }
    }
    
    datos = circle_estimation(mx,my,cnt, imp);
    x_zs_pre = datos[0];
    y_zs_pre = datos[1];
    r_zs_pre = datos[2];
    cnt = 0;
    
    // estimar esfera
    for (int h = 1; h <= zs; h++) {
      pix_est = (byte[]) stack.getPixels(h + (1 - 1) * zs);
      for (int i = 0; i < height; i++) {
        offsetaux = i * width;
        for (int j = 0; j < width; j++) {
          pos = offsetaux + j;
          if ((pix_est[pos]) > (int) factor_otsu || pix_est[pos] < 0) {
            mx[cnt] = j;
            my[cnt] = i;
            mz[cnt] = h - 1;
            cnt += 1;
          }
        }
      }
    }
    
    datos = sphere_estimation(mx, my, mz, cnt, imp);
    
    xpre = datos[0];
    ypre = datos[1];
    zpre = datos[2];
    rpre  = datos[3];

    GenericDialog gd = new GenericDialog("Factor de corte");
    gd.addSlider("Ingrese un numero entre 0 y 1", 0.0, 1.0, 0.94);
    gd.addPreviewCheckbox(pfr);
    gd.addDialogListener(this);
    gd.showDialog();
    if (gd.wasCanceled()) {
      return DONE;
    }
    if (gd.wasOKed()) {
      kkk = true;
    }
    preview = false;
    return FLAGS;
  }

  /** Listener to modifications of the input fields of the dialog.
   *  Here the parameters should be read from the input dialog.
   *  @param gd The GenericDialog that the input belongs to
   *  @param e  The input event
   *  @return whether the input is valid and the filter may be run with these parameters
   */
  public boolean dialogItemChanged (GenericDialog gd, AWTEvent e) {
    System.out.println("FLAG para DialogItemChanged ");
    
    pelar = (double) gd.getNextNumber(); 
      //System.out.println("El valor de pelar es " + pelar);
    
    return !gd.invalidNumber() && pelar >=0  &&  pelar <= 1;
  }

  /**
   * This method is called by ImageJ for processing
   * @param ip The image that should be processed
   */
  public void run (ImageProcessor ip) {
    System.out.println("FLAG para RUN ");
    
    if (preview) {
      System.out.println("Se calculara con este valor de pelado"+ pelar);  
        
      int  offset; // para buscar coordenadas en el stack
      double distancia = 0;
      //ImageStack stack_proyeccion = new ImageStack(width, height);
      ImagePlus imageprev = IJ.openVirtual(direc + imagedir);
      System.out.println("Paso 1");
        
      ImageStack stack_proyeccion = new ImageStack(width, height);    
      ImageStack stack_prev = new ImageStack(width, height);    
      
      Calibration cal = imageprev.getCalibration(); 
      double factorx_pre = cal.pixelWidth; //x contains the pixel width in units 
      double factory_pre = cal.pixelHeight; //y contains the pixel height in units 
      double factorz_pre = cal.pixelDepth; //
      
      System.out.println("Paso 2");
      ColorProcessor ipc;

      for (int h=1; h <= zs ; h++) {
        ImageProcessor histIp = new ByteProcessor(width, height);
        for (int i = 0; i < height ; i++) {
          for (int j = 0; j < width; j++) {
            imageprev.setSlice(h);
            int[] valor_imageprev = imageprev.getPixel(i,j);
            histIp.putPixelValue(i,j,valor_imageprev[0]);
            distancia = Math.sqrt(Math.pow(((j-xpre)*factorx_pre),2) + Math.pow(((i-ypre)*factory_pre),2)
            + Math.pow(((h-zpre)*factorz_pre),2));
            if (distancia > pelar * rpre) {    
              histIp.putPixelValue(i,j,0);                
            }
          }
        }  
        stack_prev.addSlice(histIp);
      }
      
      System.out.println("Valor pelar * rpre :"+ pelar * rpre);
      
      ImagePlus histIm = new ImagePlus("Nuevo test en blanco", stack_prev); 
      
      ZProjector projector = new ZProjector(histIm); 
      System.out.println("Paso 3");
      ImageProcessor proyeccion;
      projector.setMethod(ZProjector.MAX_METHOD);
      projector.setStartSlice(1 + (1-1)*zs);
      projector.setStopSlice(1*zs);
      projector.doProjection();
          
      proyeccion = projector.getProjection().getProcessor();
          
      //convertir a color para dibujar circunferencias
      ipc = proyeccion.convertToColorProcessor();
      
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
      
      //agregar slice con todos los circulos dibujadoss
      stack_proyeccion.addSlice(ipc);
      
      //  IJ.showMessage("los valores de la circunferencia son (x,y) =  ( " + xp + " , " + yp + " ) \n Con un radio de :  " + rmax);
      ImagePlus impstack = new ImagePlus("MAX_stack_hermoso", stack_proyeccion);
      impstack.show();
    }
      
    if (!preview && !kkk) {
      System.out.println("Si calculo todo sin el preview");
    }
    if (!preview && kkk) {
      System.out.println("Si calculo todo sin el preview y con ok");
      
      imp = WindowManager.getCurrentImage(); 
      
      //duplicar para no modificar stack original
      ImagePlus img2 = new Duplicator().run(imp);
      
      stack = img2.getStack();
      
      int tiempos = img2.getNFrames(); //tiempos a procesar
      
      int zs = stack.getSize()/tiempos; // numero de planos z
      int width = stack.getWidth(); //ancho de imagen
      int height = stack.getHeight(); //alto de imagen
      
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
      
      //conversion de voxeles a micrones
      Calibration cal = imp.getCalibration(); 
      double factorx = cal.pixelWidth; //x contains the pixel width in units 
      double factory = cal.pixelHeight; //y contains the pixel height in units 
      double factorz = cal.pixelDepth; //z contains the pixel (voxel) depth in units

      img2.getProcessor().setAutoThreshold(AutoThresholder.Method.Otsu, true);
        
      double threshold = img2.getProcessor().getMinThreshold();
      double factor_otsu = Math.pow(2, (int) threshold/2)/((int) threshold/2 - 1);
                
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
            if ((pix_est[pos]) > (int) factor_otsu || pix_est[pos] < 0) {
              mx[cnt] = j;
              my[cnt] = i;
              cnt += 1;
            }
          }
      }
        
      datos = circle_estimation(mx,my,cnt, imp);
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
          if ((pix_est[pos]) > (int) factor_otsu || pix_est[pos] < 0) {
              mx[cnt] = j;
              my[cnt] = i;
              cnt += 1;
          }
        }
      }
        
      datos = circle_estimation(mx,my,cnt, imp);
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
              if ((pix_est[pos]) > (int) factor_otsu || pix_est[pos] < 0) {
                mx[cnt] = j;
                my[cnt] = i;
                mz[cnt] = h-1;
                cnt += 1;
              }
            }
          }
        }
          
        datos = sphere_estimation(mx,my,mz,cnt, imp);
          
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
  
  public double dist(double[] xx, double[] yy, double width, double height, ImagePlus imp) {
    Calibration cal = imp.getCalibration(); 
    double factorx = cal.pixelWidth; //x contains the pixel width in units 
    double factory = cal.pixelHeight; //y contains the pixel height in units 
    double factorz = cal.pixelDepth; //z contains the pixel (voxel) depth in units 
        
    return (Math.sqrt(Math.pow(((xx[0]-yy[0])*factorx),2) + Math.pow(((xx[1]-yy[1])*factory),2)
            + Math.pow(((xx[2]-yy[2])*factorz),2)));
  }
  
  public double[] sphere_estimation(double[] x, double[] y, double[] z, int contador, ImagePlus imp) {
    
    //El metodo asume que los puntos no son todos coplanares
    //y que estan aproximadamente en el borde
    
    Calibration cal = imp.getCalibration(); 
    double factorx = cal.pixelWidth; //x contains the pixel width in units 
    double factory = cal.pixelHeight; //y contains the pixel height in units 
    double factorz = cal.pixelDepth; //z contains the pixel (voxel) depth in units 
      
    double Lba = 0, Lbb = 0, Lbc = 0, Lb = 0;
    double xb = 0, yb = 0, zb = 0;
    double a,b,c;
    double r = 0;

    for (int j=0; j < contador; j++) {
      xb += x[j];
      yb += y[j];
      zb += z[j];
    }

    double[] Li = new double[contador];
    xb = xb/contador;
    yb = yb/contador; 
    zb = zb/contador;
    a = xb; b = yb; c = zb;
    //IJ.showMessage("Numero de muestras tomadas:  "+ (int) contador);
    
    for (int h = 1; h <= 300; h++) {
      for (int i=0; i < contador; i++) {
          Li[i] = Math.sqrt(Math.pow((x[i]-a)*factorx,2) + Math.pow((y[i]-b)*factory,2) + Math.pow((z[i]-c)*factorz,2));
          Lba +=  (a-x[i])/(Li[i]*contador);
          Lbb +=  (b-y[i])/(Li[i]*contador);
          Lbc +=  (c-z[i])/(Li[i]*contador);
          Lb += Li[i]/contador;
      }
      a = xb + Lb*Lba;
      b = yb + Lb*Lbb;
      c = zb + Lb*Lbc;
      Lba = 0;
      Lbb = 0;
      Lbc = 0;
      Lb = 0;
    }

    for (int i=0; i < contador; i++) {
      Li[i] = Math.sqrt(Math.pow((x[i]-a)*factorx,2) + Math.pow((y[i]-b)*factory,2) + Math.pow((z[i]-c)*factorz,2));
      r += Li[i]/contador;
    }

    double[] q = {a,b,c,r};
    return q;
  }
  
  public double[] circle_estimation(double[] x, double[] y, int contador, ImagePlus imp) {
    
    Calibration cal = imp.getCalibration(); 
    double factorx = cal.pixelWidth; //x contains the pixel width in units 
    double factory = cal.pixelHeight; //y contains the pixel height in units 
    double Lba = 0, Lbb = 0, Lb = 0;
    double xb = 0, yb = 0;
    double a,b;
    double r = 0;

    for (int j=0; j < contador; j++) {
      xb += x[j];
      yb += y[j];
    }

    double[] Li = new double[contador];
    
    xb = xb/contador;
    yb = yb/contador;
    a = xb; b = yb;
    
    //IJ.showMessage("Numero de muestras tomadas:  "+ (int) contador);
    
    for (int h = 1; h <= 300; h++) {
      for (int i=0; i < contador; i++) {
          Li[i] = Math.sqrt(Math.pow((x[i]-a)*factorx,2) + Math.pow((y[i]-b)*factory,2));
          Lba +=  (a-x[i])/(Li[i]*contador);
          Lbb +=  (b-y[i])/(Li[i]*contador);
          Lb += Li[i]/contador;
      }
      a = xb + Lb*Lba;
      b = yb + Lb*Lbb;
      Lba = 0;
      Lbb = 0;
      Lb = 0;
    }

    for (int i=0; i < contador; i++) {
      Li[i] = Math.sqrt(Math.pow((x[i]-a)*factorx,2) + Math.pow((y[i]-b)*factory,2));
      r += Li[i]/contador;
    }

    double[] q = {a,b,r};
    return q;   
  }

  public static void main(String[] args) {
    // NO AGREGAR MAS LINEAS EN EL MAIN
    new ImageJ();
    System.out.println("Working Directory = " + System.getProperty("user.dir"));
    //ImagePlus image = IJ.openVirtual("Z:/Eclipse/Fijithelium/examples/Stack/video_animal1.tif");
    ImagePlus image = IJ.openVirtual("C:/Users/Alejandro/Desktop/EpitheliumProjection/examples/ejemplo_esfera.tif");

    IJ.runPlugIn(image, "Proyeccion_Esfera", "parameter=value");
    // image.show();
     WindowManager.addWindow(image.getWindow());
  }
}
