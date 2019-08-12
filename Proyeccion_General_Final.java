import ij.*;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import ij.process.*;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.TextRoi;
import ij.plugin.PlugIn; 
import ij.plugin.*; 

import java.awt.*;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.OverlayLayout;;
import javax.swing.border.EmptyBorder;

public class Proyeccion_General_Final extends JFrame implements PlugInFilter {

  private static int tiempos;
  private static String direc;
  private static String imagedir;
  private static ImagePlus image;
  private static int width;
  private static int height;
  private static int zs;
  private static int[] Xlvl;
  private static int[] Ylvl;
  
  private static int[] Inter1;
  private static int[] Inter2;
  private int[] Inter3;
  private boolean screenmod = false;
  
  static double wheel = 0;
  static double[][] corte;
  
  private static Overlay overlay;

  private static Roi roi; // puntos X para mostrar el punto de interpolacion  
  private static Roi[][] stack_roi;
  private static Roi roigrid; // Grid en rojo
  
  //private static NonBlockingGenericDialog gd;
  static ImagePlus imageprev;
  protected ImageStack stack;

  static SplineFitter sf1;   
  static SplineFitter sf2;
  static SplineFitter sf3;
  static SplineFitter sf4;
  static SplineFitter sf5;
  
  private static ImageWindow win;
  private static ImageCanvas canvas;

  private static int lvl_corte;

  private String time_corte;

  private static boolean crtlpress;
  private static boolean enable_scroll_change = false;
  
  private boolean validar_variables = true;
  private String[] tiempos_corte;
  private int[] parser;
  private static String choice = "<";

  private int parser_count=0;

  private JPanel panel;
  
  private static ImagePlus Stack_Tp;

  private static Font font;
  
  private static int currentslice = 0;
  
  private JTextField tiempos_corte_j;
  private JTextField nivel_corte;
  private JRadioButton menorrad;
  private JRadioButton mayorrad;
  private JButton cancel;
  private JButton ver_todos;
  private JButton ver_tiempos;

  private JLabel lblNivelDeCorte;
  private JLabel lblFactorDeCorte;
  private JLabel lblMostrarParaTodos;
  private JLabel lblMostrarTodosLos;
  private JLabel lblTiemposDeCorte;
  private JFrame Ventana;
  
  private boolean flag_breaker = false;
  
  private static int [][] anterior;
  private static int [][] siguiente;
  
  private static boolean vertiempos_pressed = false;
  
  private static ImageProcessor zlabel;
  
  public int setup(String arg, ImagePlus imp) {
    return STACK_REQUIRED | DOES_ALL;
  }
      
  public void run(ImageProcessor ip) {
    image = WindowManager.getCurrentImage(); 
    ImagePlus img2 = image;
    stack = img2.getStack();
    direc = img2.getOriginalFileInfo().directory;
    imagedir = img2.getOriginalFileInfo().fileName;
    imageprev = IJ.openVirtual(direc + imagedir);
    tiempos = img2.getNFrames();
    zs = stack.getSize()/tiempos;
    width = stack.getWidth();
    height = stack.getHeight();
  
    // aca empieza la magia 
    SliderWind(); 
  }
  
  /// La ventana principal del menu de seleccion de valores
  public void SliderWind() {
    Ventana = new JFrame();
    Ventana.setTitle("Plugin name");
    Ventana.setSize(400,450);
    Ventana.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    Ventana.setBounds(400, 450, 426, 482);
    Ventana.setLocationRelativeTo(null);
    Ventana.setLayout(null);
    
    lblTiemposDeCorte = new JLabel("Frames for threshold adjustment");
    lblTiemposDeCorte.setBounds(71, 42, 130, 33);
    Ventana.add(lblTiemposDeCorte);
    
    tiempos_corte_j = new JTextField();
    tiempos_corte_j.setText("1,2");
    tiempos_corte_j.setBounds(241, 47, 116, 22);
    Ventana.add(tiempos_corte_j);
    tiempos_corte_j.setColumns(10);
    
    lblNivelDeCorte = new JLabel("Initial threshold");
    lblNivelDeCorte.setBounds(71, 88, 130, 33);
    Ventana.add(lblNivelDeCorte);
    
    nivel_corte = new JTextField();
    nivel_corte.setText(String.valueOf(zs/2));
    nivel_corte.setColumns(10);
    nivel_corte.setBounds(241, 93, 116, 22);
    Ventana.add(nivel_corte);
    
    panel = new JPanel();
    panel.setBorder(null);
    panel.setBounds(38, 162, 341, 106);
    Ventana.add(panel);
    panel.setLayout(null);
    
    lblFactorDeCorte = new JLabel("Slices to keep");
    lblFactorDeCorte.setBounds(23, 34, 89, 16);
    panel.add(lblFactorDeCorte);
    
    menorrad = new JRadioButton("Keep slices before threshold");
    menorrad.addActionListener(new MenorqueActionListener());
    menorrad.setSelected(true);
    menorrad.setBounds(195, 9, 127, 25);
    panel.add(menorrad);
        
    mayorrad = new JRadioButton("Keep slices after threshold");
    mayorrad.addActionListener(new MayorqueActionListener());
    mayorrad.setBounds(195, 54, 127, 25);
    panel.add(mayorrad);
    
    lblMostrarParaTodos = new JLabel("Adjust selected frames");
    lblMostrarParaTodos.setBounds(68, 289, 189, 22);
    Ventana.add(lblMostrarParaTodos);
    
    ver_tiempos = new JButton("Adjust");
    ver_tiempos.addActionListener(new VertiemposActionListener());
    ver_tiempos.setBounds(269, 288, 97, 25);
    Ventana.add(ver_tiempos);
    
    ver_todos = new JButton("Project all frames");
    ver_todos.addActionListener(new VertodosActionListener());
    ver_todos.setBounds(103, 349, 194, 25);
    Ventana.add(ver_todos);
    ver_tiempos.requestFocus();

    Ventana.setVisible(true);
  }

  private static void dibujarLinea(int x1, int y1, int x2, int y2) {
    roigrid = new Line(x1, y1, x2, y2); 
    roigrid.setStrokeColor(Color.red);  // Primer vertical entre 100 y 709
    roigrid.setStrokeWidth(1);
    overlay.add(roigrid);
  }
    
  // Se construye el overlay con las lineas, numeros, y puntos demarcados 
  // por sobe la imagen sin afectar su integridad
  private static void llenar_overlay() {
  
    // Las marcas X de la imagen
    for (int puntosx = 0; puntosx < 3; puntosx++) {
       for (int puntosy = 0; puntosy < 3; puntosy++) {
         roi = new TextRoi(Inter1[puntosx],Inter2[puntosy],0, 0, "x" , font);
         overlay.add(roi);
         roi.setStrokeColor(Color.CYAN);
       }
    }
    
    // Las lineas
    dibujarLinea(Xlvl[0], 0, Xlvl[0], height); // Primer vertical entre 100 y 709
    dibujarLinea(Xlvl[1], 0, Xlvl[1], height); // Segunda Vertical entre 709 y 1234
    dibujarLinea(0, Ylvl[0], width, Ylvl[0]); // Primer Horizontal entre 60 y 456
    dibujarLinea(0, Ylvl[1], width, Ylvl[1]); // Segunda Horizontal entre 456 y 892
    
    // Los numeros de Z
    //1
    dibujarNumero(0, (Inter1[0]*0.1), (Inter2[0]*0.1));
    //4
    dibujarNumero(1, (Inter1[0]*0.1),Ylvl[0]+(Inter2[0]*0.1));
    //7
    dibujarNumero(2, (Inter1[0]*0.1),Ylvl[1]+(Inter2[0]*0.1));
    //2
    dibujarNumero(3, Xlvl[0]+(Inter1[0]*0.1),(Inter2[0]*0.1));
    //5
    dibujarNumero(4, Xlvl[0]+(Inter1[0]*0.1),Ylvl[0]+(Inter2[0]*0.1));
    // 8     
    dibujarNumero(5, Xlvl[0]+(Inter1[0]*0.1),Ylvl[1]+(Inter2[0]*0.1));
    //3     
    dibujarNumero(6, Xlvl[1]+(Inter1[0]*0.1),(Inter2[0]*0.1));
    //6     
    dibujarNumero(7, Xlvl[1]+(Inter1[0]*0.1),Ylvl[0]+(Inter2[0]*0.1));
    //9     
    dibujarNumero(8, Xlvl[1]+(Inter1[0]*0.1),Ylvl[1]+(Inter2[0]*0.1));    

    // slider por cuadrante
    Stack_Tp.setOverlay(overlay);
  }

  private static void dibujarNumero(int nROI, double x, double y) {
    stack_roi[currentslice][nROI] = new TextRoi(x, y, 0, 0, "z=" + String.valueOf(Math.round(corte[currentslice][nROI])), font);
    stack_roi[currentslice][nROI].setStrokeColor(Color.YELLOW);
    overlay.add(stack_roi[currentslice][nROI]);
  }

  ////Calculo de interpolacion lineal desde 9 puntos hasta completar el ancho x alto de la imagen
  // TODO: entender esto
  private static void calcular_borrado_inicial(int [] parser, int parser_count, String choice) {
    float [] vec1, vec2, vec3, vec4, vec5, vec6;
    
    //para Y = 60
    vec1 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
    vec2 = new float [] {lvl_corte,lvl_corte,lvl_corte}; //z 
    
    //para Y = 456
    vec3 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
    vec4 = new float [] {lvl_corte,lvl_corte,lvl_corte}; //z
    
    // para Y = 892
    vec5 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
    vec6 = new float [] {lvl_corte,lvl_corte,lvl_corte}; //z
    
    //PRIMERA PLANA float
    sf1 = new SplineFitter(vec1, vec2, 3);
    sf2 = new SplineFitter(vec3, vec4, 3);
    sf3 = new SplineFitter(vec5, vec6, 3);
    
    float[] vectorx0y0 = new float[width]; 
    float[] vectorx0y46 = new float[width]; 
    float[] vectorx0y89 = new float[width];
    
    zlabel = new ByteProcessor(width, height);
    zlabel.setInterpolationMethod(ImageProcessor.BILINEAR); 
    
    float[] vectoryvalue = new float[Inter2[1]]; 
    float[] vectory2value = new float[(height-Inter2[1])];
    
    // PRIMER LAYER
    for (int h = 0; h < Inter2[1] ; h++) {
      vectoryvalue[h] = h;
    }
    for (int h = 0; h < (height-Inter2[1]) ; h++) {
      vectory2value[h] = h; 
    }
    for (int x = 0; x < width ; x++) {
      float spline = (float) sf1.evalSpline(vec1,vec2,3, x); //y=0
      float spline2 = (float) sf2.evalSpline(vec3,vec4,3 ,x);//y=3
      float spline3 = (float) sf3.evalSpline(vec5,vec6,3, x);//y=3
           
      vectorx0y0 [x] =  spline;
      vectorx0y46 [x] = spline2;
      vectorx0y89 [x] = spline3;
         
      //Con los parametros de corte de Y =0 e Y=3, 
      //se pueden obtener las rectas de x1 hasta xn
      zlabel.putPixelValue(x, Inter2[0], vectorx0y0[x]);
      zlabel.putPixelValue(x, Inter2[1], vectorx0y46[x]);
      zlabel.putPixelValue(x, Inter2[2], vectorx0y89[x]);     
    }
    float [] vectorauxl1 = {Inter2[0],Inter2[1]};
    float [] vectorauxl2 = {Inter2[1],Inter2[2]};
    float [] vectorspl = new float [width];
    
    //sacar valores para todo X , Y 
    for (int X = 0; X < width ; X++) {
      float [] allxvalues = new float [2];
      allxvalues[0]= vectorx0y0[X];
      allxvalues[1]= vectorx0y46[X];
      for (int Y=0; Y < Inter2[1]; Y++) {
        sf4 = new SplineFitter(vectorauxl1,allxvalues, 2);
        float spline3 = (float) sf4.evalSpline(vectorauxl1,allxvalues,vectorauxl1.length,Y);//y=3
        if (X==100 || X==709 || X==1234) {
          zlabel.putPixelValue(X,60,255);
          zlabel.putPixelValue(X,456,255);
        }
        vectorspl [Y] = spline3;
        zlabel.putPixelValue(X,Y,vectorspl[Y]);
      }
    }
    
    //SEGUNDO LAYER
    float [] vectorspl2 = new float [width];
    float seclay = (width - Inter2[1]);
    for (int X=0; X < width ; X++) {
      float [] allx2values = new float[2];
      allx2values[0]= vectorx0y46[X];
      allx2values[1]= vectorx0y89[X];
      if (X==1234) {
        
      }
      for (int Y=0; Y < (seclay); Y++) {
        sf5 = new SplineFitter(vectorauxl2,allx2values, 2);
        float spline4 = (float) sf5.evalSpline(vectorauxl2,allx2values,vectorauxl2.length,(Y+Inter2[1]));//y=3
        if (X==100 || X==709 || X==1234) {
          zlabel.putPixelValue(X,456,255);
          zlabel.putPixelValue(X,892,255);
        }
        vectorspl2 [Y] = spline4;
        zlabel.putPixelValue(X,(Y+Inter2[1]),vectorspl2[Y]);
        if (X==1234) { 
          //System.out.println("Inicio vector Y = "+(Y+Inter2[1])+" // z="+ spline4);
        }
      }
    }
    ImageStack stack_tiempos = new ImageStack(width, height);   
    // se realiza para cada stack seleccionado el corte en la profundidad h    
    for(int z = 0; z < parser_count; z++) {
      //System.out.println("Va en el calculo de: "+ parser[z]);
      ImageStack tiempos = new ImageStack(width, height);   
      for(int h = 1; h <= zs; h++) {
        ImageProcessor nueva = new ByteProcessor(width, height);
        for (int i = 0; i < width ; i++) {
          for (int j = 0; j < height; j++) {
            imageprev.setSlice(h+(zs*(parser[z]-1))); //TIEMPO - 1
            float zcorte = zlabel.getPixel(i, j);
            int[] actualz = imageprev.getPixel(i, j);
            nueva.putPixelValue(i,j, actualz[0]);
            if (choice.equals(">")) {
              if (zcorte >= h) {
                nueva.putPixelValue(i, j, 0);
              }
            }
            if (choice.equals("<")) {
              if (zcorte <= h) {
                nueva.putPixelValue(i, j, 0);
              }
            }
          }
        }
        tiempos.addSlice(nueva);  
      }
      ImagePlus Tp = new ImagePlus("Stack por tiempos seleccionados", tiempos);
      ZProjector projectar_t = new ZProjector(Tp); 
      projectar_t.setMethod(ZProjector.MAX_METHOD);
      projectar_t.setStartSlice(1);
      projectar_t.setStopSlice(zs);
      projectar_t.doProjection();
      
      ImagePlus projection_t = projectar_t.getProjection();
      stack_tiempos.addSlice(projection_t.getProcessor());          
    }
    Stack_Tp = new ImagePlus("Adjust threshold by quadrant", stack_tiempos);
    Stack_Tp.show();
    // TODO: sliders para elegir zs
  }

  //////////////////// MODIFICAR PREVIE TIEMPOS CON SCROLL , ///////////////////////
  ////////////////////    lo mismo que el anterior pero se llama cada vez que hay un cambio   ////////////////
  private static void calcular_borrado_scroll (int [] parser, int parser_count, String choice) {
    float [] vec1, vec2, vec3, vec4, vec5, vec6;
    
    //para Y = 60
    vec1 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
    vec2 = new float [] {(float) corte[currentslice][0],(float) corte[currentslice][3],(float) corte[currentslice][6]}; //z 
        
    //para Y = 456
    vec3 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
    vec4 = new float [] {(float) corte[currentslice][1],(float) corte[currentslice][4],(float) corte[currentslice][7]}; //z
    
    // para Y = 892
    vec5 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
    vec6 = new float [] {(float) corte[currentslice][2],(float) corte[currentslice][5],(float) corte[currentslice][8]}; //z
    
    //PRIMERA PLANA float
    sf1 = new SplineFitter(vec1, vec2, 3);
    sf2 = new SplineFitter(vec3, vec4, 3);
    sf3 = new SplineFitter(vec5, vec6, 3);
    float [] vectorx0y0 = new float[width]; 
    float [] vectorx0y46 = new float[width]; 
    float [] vectorx0y89 = new float[width];
    zlabel = new ByteProcessor(width, height);
    zlabel.setValue(255); // white = 255
    zlabel.fill(); 
    zlabel.setInterpolationMethod(ImageProcessor.BILINEAR); 

    float [] vectoryvalue = new float[Inter2[1]]; 
    float [] vectory2value = new float[(height-Inter2[1])];

    // PRIMER LAYER
    for (int h = 0; h < Inter2[1]; h++) {
      vectoryvalue[h]= h; 
    }
    for (int h = 0; h < (height-Inter2[1]); h++) {
      vectory2value[h]= h; 
    }
    for(int h = 0; h < width ; h++) {
      float spline = (float) sf1.evalSpline(vec1,vec2,vec1.length,h); //y=0
      float spline2 = (float) sf2.evalSpline(vec3,vec4,vec3.length,h);//y=3
      float spline3 = (float) sf3.evalSpline(vec5,vec6,vec5.length,h);//y=3
           
      vectorx0y0 [h] =  spline;
      vectorx0y46 [h] = spline2;
      vectorx0y89 [h] = spline3;
      
      //Con los parametros de corte de Y =0 e Y=3, 
      //se pueden obtener las rectas de x1 hasta xn
      zlabel.putPixelValue(h,Inter2[0],vectorx0y0[h]);
      zlabel.putPixelValue(h,Inter2[1],vectorx0y46[h]);
      zlabel.putPixelValue(h,Inter2[2],vectorx0y89[h]);
    }
    float [] vectorauxl1 = {Inter2[0],Inter2[1]};
    float [] vectorauxl2 = {Inter2[1],Inter2[2]};
    float [] vectorspl = new float [width];
    
    //sacar valores para todo X , Y 
    for(int X=0; X < width ; X++) {
      float [] allxvalues = new float [2];
      allxvalues[0]= vectorx0y0[X];
      allxvalues[1]= vectorx0y46[X];
      for(int Y=0; Y < Inter2[1] ; Y++) {
        sf4 = new SplineFitter(vectorauxl1,allxvalues, 2);
        float spline3 = (float) sf4.evalSpline(vectorauxl1,allxvalues,vectorauxl1.length,Y);//y=3
        if (X==100 || X==709 || X==1234) {
          zlabel.putPixelValue(X,60,255);
          zlabel.putPixelValue(X,456,255);
         }
         vectorspl [Y] = spline3;
         zlabel.putPixelValue(X,Y,vectorspl[Y]);
      }
    }
    
    //SEGUNDO LAYER
    float [] vectorspl2 = new float [width];
    float seclay = (width - Inter2[1]);
    for(int X=0; X < width ; X++) {
      float [] allx2values = new float [2];
      allx2values[0]= vectorx0y46[X];
      allx2values[1]= vectorx0y89[X];
      for(int Y=0; Y < (seclay) ; Y++) {
        sf5 = new SplineFitter(vectorauxl2,allx2values, 2);
        float spline4 = (float) sf5.evalSpline(vectorauxl2,allx2values,vectorauxl2.length,(Y+Inter2[1]));//y=3
        if (X==100 || X==709 || X==1234) {
          zlabel.putPixelValue(X,456,255);
          zlabel.putPixelValue(X,892,255);
        }
        vectorspl2 [Y] = spline4;
        zlabel.putPixelValue(X,(Y+Inter2[1]),vectorspl2[Y]);
        if (X==1234) {
        }
      }
    }
    
    ImageStack tiempos = new ImageStack(width, height);   
    for(int h = 1; h <= zs ; h++) {
      ImageProcessor nueva = new ByteProcessor(width, height);
      for (int i = 0; i < width; i++) {
        for ( int j = 0; j < height; j++) {
          imageprev.setSlice(h+(zs*(parser[currentslice]-1))); //TIEMPO - 1
          float zcorte = zlabel.getPixel(i, j);
          int[] actualz = imageprev.getPixel(i, j);
          nueva.putPixelValue(i,j, actualz[0]);
          if (choice.equals(">")) {
            if (zcorte >= h ) {
              nueva.putPixelValue(i, j, 0);
            }
          }
          if (choice.equals("<")) {
            if (zcorte <= h) {
              nueva.putPixelValue(i, j, 0);
            }
          }
        }
      }
      tiempos.addSlice(nueva);
    }  
    
    ImagePlus Tp = new ImagePlus("Stack por tiempos seleccionados", tiempos);
    ZProjector projectar_t = new ZProjector(Tp); 
    projectar_t.setMethod(ZProjector.MAX_METHOD);
    projectar_t.setStartSlice(1 + (1-1)*zs);
    projectar_t.setStopSlice(1*zs);
    projectar_t.doProjection();
    ImagePlus projection_t = projectar_t.getProjection();
    for (int x = 0; x < width; x++) {
      for ( int y=0; y<height;y++) {
        int[] pixel_update_scroll = projection_t.getPixel(x,y);
        Stack_Tp.getProcessor().putPixelValue(x,y,(pixel_update_scroll[0]));
      }
    }
  }
  
  /// Misma funcion, considerando que no se seleccionaron tiempos antes.
  private static void calcular_borrado_totalsintiempo(int [] parser, int parser_count, String choice) {      
    ImageStack stack_tiempos = new ImageStack(width, height);   
    for(int z = 0; z < tiempos; z++) {
      //System.out.println("Va en el calculo de: "+ parser[z]);
      ImageStack tiempos = new ImageStack(width, height);   
      for(int h = 1; h <= zs; h++) {
        ImageProcessor nueva = new ByteProcessor(width, height);
        for (int i = 0; i < width; i++ ) {
          for (int j = 0; j < height; j++) {
            imageprev.setSlice(h + (zs * z));
            float zcorte = lvl_corte;
            int[] actualz = imageprev.getPixel(i, j);
            nueva.putPixelValue(i,j, actualz[0]);
            if (choice.equals(">")) {
              if (zcorte >= h) {
                nueva.putPixelValue(i, j, 0);
              }
            }
            if (choice.equals("<")) {
              if (zcorte <= h) {
                nueva.putPixelValue(i, j, 0);
              }
            }
          }
        }
        tiempos.addSlice(nueva);  
      }
      ImagePlus Tp = new ImagePlus("Stack por tiempos seleccionados", tiempos);
      ZProjector projectar_t = new ZProjector(Tp); 
      projectar_t.setMethod(ZProjector.MAX_METHOD);
      projectar_t.setStartSlice(1 + (1-1)*zs);
      projectar_t.setStopSlice(1*zs);
      projectar_t.doProjection();
      ImagePlus projection_t = projectar_t.getProjection();
      stack_tiempos.addSlice(projection_t.getProcessor());
    }
    Stack_Tp = new ImagePlus("Max_Stack_all", stack_tiempos);
    Stack_Tp.show();
  }
    
////////////////////  Calculo en caso de seleccionar tiempos y editar estos ///////////////////
  private static void calcular_borrado_total (int [] parser, int parser_count, String choice) {
    ImageStack zlabel_stack = new ImageStack(width, height);
    float [] vec1_all_time, vec2_all_time;
    for (int x = 0; x < parser_count; x++) {
      float [] vec1, vec2, vec3, vec4, vec5, vec6;
      
      //para Y = 60
      vec1 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
      vec2 = new float [] {(float) corte[x][0],(float) corte[x][3],(float) corte[x][6]}; //z 
      
      //para Y = 456
      vec3 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
      vec4 = new float [] {(float) corte[x][1],(float) corte[x][4],(float) corte[x][7]}; //z
      
      // para Y = 892
      vec5 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
      vec6 = new float [] {(float) corte[x][2],(float) corte[x][5],(float) corte[x][8]}; //z
      
      //PRIMERA PLANA float
      sf1 = new SplineFitter(vec1, vec2, 3);
      sf2 = new SplineFitter(vec3, vec4, 3);
      sf3 = new SplineFitter(vec5, vec6, 3);
      
      float [] vectorx0y0 = new float[width]; 
      float [] vectorx0y46 = new float[width]; 
      float [] vectorx0y89 = new float[width];
       
      ImageProcessor zlabel_times = new ByteProcessor(width, height);  
      zlabel_times.setValue(255); // white = 255 
      zlabel_times.fill(); 
      zlabel_times.setInterpolationMethod(ImageProcessor.BILINEAR); 
      float [] vectoryvalue = new float[Inter2[1]]; 
      float [] vectory2value = new float[(height-Inter2[1])];
       
      // PRIMER LAYER
      for(int h = 0; h < Inter2[1] ; h++) {
        vectoryvalue [h] = h; 
      }
      for(int h = 0; h < (height-Inter2[1]) ; h++) {
        vectory2value [h] = h;
      }
      for(int h = 0; h < width ; h++) {
         float spline = (float) sf1.evalSpline(vec1,vec2,vec1.length,h); //y=0
         float spline2 = (float) sf2.evalSpline(vec3,vec4,vec3.length,h);//y=3
         float spline3 = (float) sf3.evalSpline(vec5,vec6,vec5.length,h);//y=3
          
         vectorx0y0 [h] =  spline;
         vectorx0y46 [h] = spline2;
         vectorx0y89 [h] = spline3;
         
         //Con los parametros de corte de Y =0 e Y=3, 
         //se pueden obtener las rectas de x1 hasta xn
         zlabel_times.putPixelValue(h,Inter2[0],vectorx0y0[h]);
         zlabel_times.putPixelValue(h,Inter2[1],vectorx0y46[h]);
         zlabel_times.putPixelValue(h,Inter2[2],vectorx0y89[h]);  
      }
      
      float [] vectorauxl1 = {Inter2[0],Inter2[1]};
      float [] vectorauxl2 = {Inter2[1],Inter2[2]};
      float [] vectorspl = new float [width];
      
      //sacar valores para todo X , Y 
      for(int X = 0; X < width ; X++) {
        float [] allxvalues = new float[2];
        allxvalues[0]= vectorx0y0[X];
        allxvalues[1]= vectorx0y46[X];
        for (int Y = 0; Y < Inter2[1]; Y++) {
          sf4 = new SplineFitter(vectorauxl1,allxvalues, 2);
          float spline3 = (float) sf4.evalSpline(vectorauxl1,allxvalues,vectorauxl1.length,Y);//y=3
          if (X==100 || X==709 || X==1234) {
            zlabel_times.putPixelValue(X,60,255);
            zlabel_times.putPixelValue(X,456,255);
          }
          vectorspl [Y] = spline3;
          zlabel_times.putPixelValue(X,Y,vectorspl[Y]);             
        }
      }
     
      //SEGUNDO LAYER
      float [] vectorspl2 = new float [width];
      float seclay = (width - Inter2[1]);
      for(int X = 0; X < width ; X++) {
        float [] allx2values = new float[2];
        allx2values[0]= vectorx0y46[X];
        allx2values[1]= vectorx0y89[X];
        for (int Y = 0; Y < (seclay) ; Y++) {
          sf5 = new SplineFitter(vectorauxl2,allx2values, 2);
          float spline4 = (float) sf5.evalSpline(vectorauxl2,allx2values,vectorauxl2.length,(Y+Inter2[1]));//y=3
          if (X==100 || X==709 || X==1234) {
            zlabel_times.putPixelValue(X,456,255);
            zlabel_times.putPixelValue(X,892,255);
          }
          vectorspl2 [Y] = spline4;
          zlabel_times.putPixelValue(X,(Y+Inter2[1]),vectorspl2[Y]);
          if(X==1234) { 
            //System.out.println("Inicio vector Y = "+(Y+Inter2[1])+" // z="+ spline4);
          }
        }
      }
      
      zlabel_stack.addSlice(zlabel_times);
    //////////////////////////// FIN DEL CICLO POR TIEMPO
    }
     
    ImagePlus todo_label = new ImagePlus ("asdasd",zlabel_stack);
    ImageStack all_label = new ImageStack(width, height);
    for (int r = 0; r < tiempos; r++) {
      ImageProcessor every_label = new ByteProcessor(width, height);   
      every_label.setValue(255); // white = 255 
      every_label.fill();
      all_label.addSlice(every_label);
    }
    
    ImagePlus final_all = new ImagePlus ("Max_Stack_all",all_label);
    vec1_all_time = new float [parser_count];
    vec2_all_time = new float [parser_count];
    for (int x = 0; x < parser_count; x++) {
      vec1_all_time[x]= parser[x];
    }
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        for (int f = 1; f <= parser_count; f++) {
          todo_label.setSlice(f);
          vec2_all_time [f-1] =  (float) todo_label.getProcessor().getPixelValue(x, y);
        }  
        SplineFitter fitperpixel;
        fitperpixel = new SplineFitter(vec1_all_time, vec2_all_time, parser_count);
        for (int h = 1; h <= tiempos ; h++) {
          final_all.setSlice(h);
          float spline_bypixel = (float) fitperpixel.evalSpline(vec1_all_time,vec2_all_time,vec1_all_time.length,h); //y=0
          if (spline_bypixel <= 0) {
            final_all.getProcessor().putPixelValue(x,y, 1);
          }
          else {
            if(spline_bypixel >= zs) {
              final_all.getProcessor().putPixelValue(x,y, zs);
            }
            else {
              final_all.getProcessor().putPixelValue(x,y, spline_bypixel);
            }
          }
        }
      }
    }
      
    //// ACA EMPIEZAN EL BORRADO DE TODAS // Z 
    ImageStack todos_tiempos = new ImageStack(width, height);   
    for (int z=0; z < tiempos ; z++) {
      //System.out.println("Va en el calculo de: "+ parser[z]);
      ImageStack tiempos = new ImageStack(width, height);   
      for(int h=1; h <= zs ; h++) {
        ImageProcessor nueva = new ByteProcessor(width, height);
        for (int i = 0; i < width ; i++) {
          for (int j = 0; j < height; j++) {
            imageprev.setSlice(h + (zs * z)); //TIEMPO - 1
            final_all.setSlice(z + 1);
            
            int[] zcorte = final_all.getPixel(i, j);
            int[] actualz = imageprev.getPixel(i, j);
            
            nueva.putPixelValue(i,j, actualz[0]);
            if (choice.equals(">")) {
              if (zcorte[0] >= h) {
                nueva.putPixelValue(i, j, 0);
              }
            }
            if (choice.equals("<")) {
              if (zcorte[0] <= h) {
                nueva.putPixelValue(i, j, 0);
              }
            }
          }
        }
        tiempos.addSlice(nueva);  
      }
      ImagePlus Tp = new ImagePlus("Stack por tiempos seleccionados", tiempos);
      ZProjector projectar_t = new ZProjector(Tp); 
      projectar_t.setMethod(ZProjector.MAX_METHOD);
      projectar_t.setStartSlice(1 + (1-1)*zs);
      projectar_t.setStopSlice(1*zs);
      projectar_t.doProjection();
      
      ImagePlus projection_t = projectar_t.getProjection();
      todos_tiempos.addSlice(projection_t.getProcessor());
    }
    ImagePlus All = new ImagePlus("Imagen Stack Tiempos", todos_tiempos);
    All.show();
  }
  
  private static void ShowWinCanvas() {
    
  }

  class MyWindow extends JFrame implements MouseMotionListener {

    private UICuadrante uiCuadrante;
    private ContenedorImagen contenedorImagen;
    private final int anchoUICuadrante = 20;
    private final int altoUICuadrante = 60;
    private int[][] zEnCuadrante;
    private int numeroDeFramesSeleccionados = 1;
    private int frameActual = 0;
    private int cuadranteActual = 0;
    private final int nCuadrantes = 9;

    public MyWindow(ImagePlus image)  {
      this.setTitle("Prueba OverlayLayout");
      this.zEnCuadrante = new int[numeroDeFramesSeleccionados][nCuadrantes];

      uiCuadrante = new UICuadrante();
      uiCuadrante.setBounds(width / 3, 0, anchoUICuadrante, altoUICuadrante);

      contenedorImagen = new ContenedorImagen(image, this);
      contenedorImagen.setBounds(0, 0, width, height);

      JLayeredPane layered = new JLayeredPane();
      layered.setPreferredSize(new Dimension(width, height));
      layered.add(contenedorImagen, new Integer(1));
      layered.add(uiCuadrante, new Integer(2));

      this.setLayout(new BorderLayout());
      this.add(new JLabel("test"), BorderLayout.NORTH);
      this.add(layered, BorderLayout.CENTER);
      this.pack();
      this.setVisible(true);
    }

    public void mouseMoved(MouseEvent e) {
      int
        xCuadrante = (int)(3 * e.getX() / width),
        yCuadrante = (int)(3 * e.getY() / height),
        xUICuadrante = xCuadrante * width / 3,
        yUICuadrante = yCuadrante * height / 3;
      cuadranteActual = xCuadrante + yCuadrante * 3;
      uiCuadrante.setBounds(
        xUICuadrante,
        yUICuadrante,
        anchoUICuadrante,
        altoUICuadrante
      );
      System.out.println("El mouse esta sobre el cuadrante " + cuadranteActual);
      imprimirZs();
    }

    private void imprimirZs() {
      for (int i = 0; i < zEnCuadrante.length; i++) {
        for (int j = 0; j < zEnCuadrante[0].length; j++) {
          if ((j + 1) % 3 == 0) {
            System.out.println(zEnCuadrante[i][j]);
          }
          else {
            System.out.print(zEnCuadrante[i][j] + ",");
          }
        }
      }
    }

    public void mouseDragged(MouseEvent e) {
    }

    private class ContenedorImagen extends JPanel {
      
      public ContenedorImagen(ImagePlus image, MouseMotionListener listener) {
        setMaximumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        ImageCanvas ic = new ImageCanvas(image);
        ic.addMouseMotionListener(listener);
        add(ic);
      }
    }

    private class UICuadrante extends JPanel implements ActionListener {

      private JButton botonAumentarZ, botonReducirZ;

      public UICuadrante() {
        setBackground(Color.RED);
        setMaximumSize(new Dimension(anchoUICuadrante, altoUICuadrante));
        setPreferredSize(new Dimension(anchoUICuadrante, altoUICuadrante));
        botonAumentarZ = new JButton("+");
        botonAumentarZ.setPreferredSize(new Dimension(20, 15));
        botonAumentarZ.addActionListener(this);
        botonReducirZ = new JButton("-");
        botonReducirZ.setPreferredSize(new Dimension(20, 15));
        botonReducirZ.addActionListener(this);
        JLabel slider = new JLabel();
        slider.setPreferredSize(new Dimension(20, 20));
        add(botonAumentarZ);
        add(slider);
        add(botonReducirZ);
      }

      public void paintComponent(Graphics g) {
        g.setColor(Color.red);
        g.fillOval(75, 75, 150, 75);
      }

      public void actionPerformed(ActionEvent e) {
        JButton origen = (JButton)(e.getSource());
        switch (origen.getText()) {
          case "+": {
            zEnCuadrante[frameActual][cuadranteActual] = Math.min(
              zEnCuadrante[frameActual][cuadranteActual] + 1,
              zs
            );
            break;
          }
          case "-": {
            zEnCuadrante[frameActual][cuadranteActual] = Math.max(
              zEnCuadrante[frameActual][cuadranteActual] - 1,
              0
            );
            break;
          }
        }
      }
    }

  }

  /// Lo que permite la interaccion del cursor y las capas es esta clase
  static class Window extends StackWindow implements MouseWheelListener {

    public Window(ImagePlus Stack_Tp, ImageCanvas canvas) { 
      super(Stack_Tp, canvas);
      addMouseWheelListener(this);
    }

    private void cambiarCorteCuadrante(int zoom, int cuadrante) {
      int intcadena = (int) (corte[currentslice][cuadrante]);
      String cadena = String.valueOf(intcadena);  
      if (zoom < 0 && intcadena < zs) {
        corte[currentslice][cuadrante] += 1;
      }
      else if (intcadena > 1) {
        corte[currentslice][cuadrante] -= 1;
      }
      overlay.clear();
      llenar_overlay();
    }

    public synchronized void mouseWheelMoved(MouseWheelEvent event) {
      if (getCanvas().cursorOverImage()) {
        Point point = getCanvas().getMousePosition();
        int zoom = event.getWheelRotation();
        double magnificacion = (double) getCanvas().getMagnification();
        int Xcursor = (int) (((point.getX() * 1)) / magnificacion);
        int Ycursor = (int) (((point.getY() * 1)) / magnificacion);
        if (Xcursor>=0 && Xcursor<= Xlvl[0] && Ycursor>=0 && Ycursor<= Ylvl[0] && crtlpress) {
          cambiarCorteCuadrante(zoom, 0);
        }
        if (Xcursor>=Xlvl[0] && Xcursor<= Xlvl[1] && Ycursor>=0 && Ycursor<= Ylvl[0] && crtlpress) {
          cambiarCorteCuadrante(zoom, 3);
        }
        if (Xcursor>=Xlvl[1] && Xcursor<= width && Ycursor>=0 && Ycursor<= Ylvl[0] && crtlpress) {
          cambiarCorteCuadrante(zoom, 6);
        }
        if (Xcursor>=0 && Xcursor<= Xlvl[0] && Ycursor>=Ylvl[0] && Ycursor<= Ylvl[1] && crtlpress) {
          cambiarCorteCuadrante(zoom, 1);
        }
        if (Xcursor>=Xlvl[0] && Xcursor<= Xlvl[1] && Ycursor>=Ylvl[0] && Ycursor<= Ylvl[1] && crtlpress) {
          cambiarCorteCuadrante(zoom, 4);
        }
        if (Xcursor>=Xlvl[1] && Xcursor<= width && Ycursor>=Ylvl[0] && Ycursor<= Ylvl[1] && crtlpress) {
          cambiarCorteCuadrante(zoom, 7);
        }
        if (Xcursor>=0 && Xcursor<= Xlvl[0] && Ycursor>=Ylvl[1] && Ycursor<= height && crtlpress) {
          cambiarCorteCuadrante(zoom, 2);
        }
        if (Xcursor>=Xlvl[0] && Xcursor<= Xlvl[1] && Ycursor>=Ylvl[1] && Ycursor<= height && crtlpress) {
          cambiarCorteCuadrante(zoom, 5);
        }
        if (Xcursor>=Xlvl[1] && Xcursor<= width && Ycursor>=Ylvl[1] && Ycursor<= height && crtlpress) {
          cambiarCorteCuadrante(zoom, 8);
        }
      }
    }
  }

  // Empiezan los listener, por boton, por valor, por accion y por proceso.
  private class VertodosActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      if(!vertiempos_pressed) {  
        time_corte = tiempos_corte_j.getText();
        lvl_corte = Integer.valueOf(nivel_corte.getText());
        tiempos_corte = time_corte.split(",");
        if (lvl_corte > 0 && lvl_corte< zs) {
          flag_breaker = false;
          validar_variables = false;
        }
        else {
          IJ.showMessage("El nivel de corte ingresado es incorrecto \nDebe ser un valor entre "+ 0 + " y "+ zs);
        }
        
        int sizethis= tiempos_corte.length;
        parser = new int[sizethis];
        for(int x =0; x < sizethis; x++) {
          if(!tiempos_corte[x].equals(" ") && !tiempos_corte[x].equals("")) {
            parser[parser_count] = Integer.parseInt(tiempos_corte[x]);  
            parser_count++;  
          }
        }
        
        //System.out.println("En realidad los numeros son "+ parser_count  );
        enable_scroll_change = true;
        corte = new double[parser_count][9];
        for (int tps = 0; tps < parser_count; tps++) {
          for (int points = 0; points < 9; points++) {
            corte[tps][points] = lvl_corte;              
          }
        }
          
        anterior = new int [parser_count][9];
        siguiente = new int [parser_count][9];
        
        for (int x = 0; x<parser_count; x++) {
          for (int y = 0; y<9;y++) {
            anterior [x][y]= lvl_corte;
            siguiente [x][y]= lvl_corte;
          }
        }
        
        //////// CICLO PARA GENERAR TODOS LOS TIEMPOS 
        Xlvl = new int [2];
        Ylvl = new int [2];
        Inter1 = new int [3];
        Inter2 = new int [3];
        Inter3 = new int [3];
        
        int Xsetpoints = width/6;
        int Ysetpoints = height/6;
           
        Xlvl[0] = Xsetpoints * 2;
        Xlvl[1] = (Xsetpoints * 4);
        Ylvl[0] = Ysetpoints * 2;
        Ylvl[1] = (Ysetpoints * 4);
        
        Inter1[0] = Xsetpoints;
        Inter1[1] = Xsetpoints * 3;
        Inter1[2] = Xsetpoints * 5;
        
        Inter2[0] = Ysetpoints;
        Inter2[1] = Ysetpoints * 3;
        Inter2[2] = Ysetpoints * 5;
          
        calcular_borrado_totalsintiempo(parser,parser_count,choice);  
        screenmod = true;
        Ventana.setVisible(false);
        return;
      }
      
      else {
        if(vertiempos_pressed) {
          calcular_borrado_total(parser,parser_count,choice);
          Ventana.setVisible(false);
          Stack_Tp.close();
          return;
        }
      }
    }
  }
  
  private class VertiemposActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      vertiempos_pressed = true;
      time_corte = tiempos_corte_j.getText();
      lvl_corte = Integer.valueOf(nivel_corte.getText());
      tiempos_corte = time_corte.split(",");
      if (lvl_corte > 0 && lvl_corte< zs) {
        flag_breaker=false;
        validar_variables = false;
      }
      else {
        IJ.showMessage("El nivel de corte ingresado es incorrecto \nDebe ser un valor entre "+ 0 + " y "+ zs);
      }
      
      int sizethis= tiempos_corte.length;
      parser = new int[sizethis];
      
      for(int x = 0; x < sizethis; x++) {
        if(!tiempos_corte[x].equals(" ") && !tiempos_corte[x].equals("")) {
          parser[parser_count] = Integer.parseInt(tiempos_corte[x]);
          parser_count++;
        }
      }
      enable_scroll_change = true;
      corte = new double[parser_count][9];
      for (int tps = 0; tps < parser_count; tps++) {
        for (int points = 0; points < 9; points++) {
          corte[tps][points] = lvl_corte;            
        }
      }
      anterior = new int [parser_count][9];
      siguiente = new int [parser_count][9];
      for (int x = 0; x < parser_count; x++) {
        for (int y = 0; y < 9;y++) {
          anterior [x][y]= lvl_corte;
          siguiente [x][y]= lvl_corte;
        }
      }
      
      //////// CICLO PARA GENERAR TODOS LOS TIEMPOS 
      Xlvl = new int [2];
      Ylvl = new int [2];
      
      Inter1 = new int [3];
      Inter2 = new int [3];
      Inter3 = new int [3];
      
      int Xsetpoints = width/6;
      int Ysetpoints = height/6;
      
      Xlvl[0] = Xsetpoints * 2;
      Xlvl[1] = (Xsetpoints * 4);
      Ylvl[0] = Ysetpoints * 2;
      Ylvl[1] = (Ysetpoints * 4);
      
      Inter1[0] = Xsetpoints;
      Inter1[1] = Xsetpoints * 3;
      Inter1[2] = Xsetpoints * 5;

      Inter2[0] = Ysetpoints;
      Inter2[1] = Ysetpoints * 3;
      Inter2[2] = Ysetpoints * 5;
      
      calcular_borrado_inicial(parser,parser_count,choice);
      ZProjector projector = new ZProjector(imageprev); 
      projector.setMethod(ZProjector.MAX_METHOD);
      projector.setStartSlice(1 + (1-1)*zs);
      projector.setStopSlice(1*zs);
      projector.doProjection();
      ImagePlus projections = projector.getProjection();
      new MyWindow(Stack_Tp);
      Stack_Tp.setWindow(new Window(Stack_Tp, Stack_Tp.getCanvas())); 
      overlay = new Overlay();
      font = new Font("", Font.PLAIN, 24);
      
      ////////////////////////////////////
      ///// AGREGAR LA SLICEROI
      stack_roi = new Roi[parser_count][9]; 
      llenar_overlay();
      win = Stack_Tp.getWindow();
      canvas = win.getCanvas();
      win.removeKeyListener(IJ.getInstance());
      canvas.removeKeyListener(IJ.getInstance());
      win.addKeyListener(new WinKeyListener());
      canvas.addKeyListener(new WinKeyListener());
      ImagePlus.addImageListener(new IpImageListener());
      screenmod = true;
      lblTiemposDeCorte.setEnabled(false);
      tiempos_corte_j.setEnabled(false);
      lblNivelDeCorte.setEnabled(false);
      nivel_corte.setEnabled(false);
      lblFactorDeCorte.setEnabled(false);
      menorrad.setEnabled(false);
      mayorrad.setEnabled(false);
      lblMostrarParaTodos.setEnabled(false);
      ver_tiempos.setEnabled(false);
    }  
  }
    
  private class MenorqueActionListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      if (menorrad.isSelected()) {
        mayorrad.setSelected(false);
        choice = "<";
      }
    }
  }

  private class MayorqueActionListener implements ActionListener {

    public void actionPerformed(ActionEvent e)  {
      if(mayorrad.isSelected()) {
        menorrad.setSelected(false);
        choice=">";
      }
    }
  }

  private class WinKeyListener implements KeyListener {

    @Override
    public void keyPressed(KeyEvent e) {
      int keyCode = e.getKeyCode();
      char keyChar = e.getKeyChar();
      int flags = e.getModifiers();
      String keytex = KeyEvent.getKeyText(keyCode);
      if (keytex.equals("Ctrl")) {
        crtlpress =  true;
        for (int x = 0; x < 9; x++) {
          anterior[currentslice][x] = siguiente[currentslice][x];
        }
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      int keyCode = e.getKeyCode();
      String keytex = KeyEvent.getKeyText(keyCode);
      if (keytex.equals("Ctrl")) {
        crtlpress =  false;
        for (int x = 0; x < 9; x++) {
          siguiente[currentslice][x] = (int) corte[currentslice][x];
        }
        if(anterior[currentslice][0] != siguiente[currentslice][0] || anterior[currentslice][1] != siguiente[currentslice][1] || anterior[currentslice][2] != siguiente[currentslice][2] 
            || anterior[currentslice][3] != siguiente[currentslice][3] || anterior[currentslice][4] != siguiente[currentslice][4] || 
            anterior[currentslice][5] != siguiente[currentslice][5] || anterior[currentslice][6] != siguiente[currentslice][6] || 
            anterior[currentslice][7] != siguiente[currentslice][7] || anterior[currentslice][8] != siguiente[currentslice][8]) {
          long time_start, time_end;
          time_start = System.currentTimeMillis();
          calcular_borrado_scroll(parser,parser_count,choice);
          time_end = System.currentTimeMillis();
          System.out.println("Tiempo "+ ( time_end - time_start )+" mili");
        }
      }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }   
  }
  
  private class IpImageListener implements ImageListener {

    @Override
    public void imageClosed(ImagePlus e) {
    }

    @Override
    public void imageOpened(ImagePlus e) {
    }

    @Override
    public void imageUpdated(ImagePlus e) {
      currentslice= ((Stack_Tp.getCurrentSlice()) -1);
      overlay.clear();
      llenar_overlay();
    }
  }

  public static void main(String[] args) {
    new ImageJ();
    System.out.println("Working Directory = " + System.getProperty("user.dir"));
    ImagePlus image = IJ.openVirtual("C:/Users/Alejandro/Desktop/java/epithelium-projection/examples/sshort sequence-1.tif");      
    IJ.runPlugIn(image, "Proyeccion_General_Final", "parameter=value");
    WindowManager.addWindow(image.getWindow());
  }
}
