import ij.*;
import java.util.Observable;
import java.util.Observer;

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



import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent; 

import java.util.Vector;


import java.awt.*;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;






public class Proyeccion_General_Final extends JFrame implements PlugInFilter, KeyListener, ImageListener, Observer{
    
 
	/**
	 * 
	 */
	private static final long serialVersionUID = 4200370641327139174L;

	static double scroll;
	
	private static int tiempos;
	private static String direc;
	private static String imagedir;
	private static ImagePlus image;
	private static int width;
	private static int height;
	private static int zs;
	private static int [] Xlvl;
	private static int [] Ylvl;
	
	private static int [] Inter1;
	private static int [] Inter2;
	private int [] Inter3;
	private boolean screenmod = false;
	
	static double wheel = 0;
	static double [] [] roll;
	
	private static Robot robot = null;

	
	private static Overlay overlay;
	private static Overlay crossverlay;
	private static Overlay numberoverlay;

	private static Roi roi; // puntos X para mostrar el punto de interpolacion
	
	private static Roi roic1; //
	private static Roi roic2; //
	private static Roi roic3; //
	private static Roi roic4; // rois para
	private static Roi roic5; // mostrar el 
	private static Roi roic6; // valor de Z
	private static Roi roic7; //
	private static Roi roic8; //
	private static Roi roic9; //
	
	private static Roi [] [] stack_roi; 
	
	
	private static Roi roigrid; // Grid en rojo
	
	//private static NonBlockingGenericDialog gd;
	static ImagePlus imageprev;
	
	protected ImageStack stack;
	private ImagePlus imp;

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
    private int [] parser;
    private String choice = "<";
    
	////////////////////////////////////////////////////////

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
	
	


	
	
	public int setup(String arg, ImagePlus imp){
		
		return STACK_REQUIRED | DOES_ALL ;
		
	   	}
      	
    	
    public void run (ImageProcessor ip) {
    	//System.out.println("FLAG para RUN ");
    	
    	image = WindowManager.getCurrentImage(); 
    	
    	ImagePlus img2 = image;
    	
		stack = img2.getStack();
		
		direc = img2.getOriginalFileInfo().directory;

		imagedir = img2.getOriginalFileInfo().fileName;
		
		imageprev = IJ.openVirtual(direc + imagedir);
			
		tiempos = img2.getNFrames(); //tiempos a procesar
		
		 zs = stack.getSize()/tiempos; // numero de planos z
		 width = stack.getWidth(); //ancho de imagen
		 height = stack.getHeight(); //alto de imagen
		
		  
	     try {
			robot = new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    
	
	     // aca empieza la magia
	     
	     SliderWind();
	    
			
	     
    }
    
    
    // Se construye el overlay con las lineas, numeros, y puntos demarcados 
    // por sobe la imagen sin afectar su integridad
    
    private static void llenar_overlay() {
		// TODO Auto-generated method stub
		  
    	
    	// Las marcas X de la imagen
    	
    	 for (int puntosx = 0; puntosx < 3; puntosx ++ )
   	  {
   		  for (int puntosy = 0; puntosy < 3; puntosy ++ )
       	  {
   			  roi = new TextRoi(Inter1[puntosx],Inter2[puntosy],0, 0, "x" , font);
   	    	  overlay.add(roi);
   	     	  roi.setStrokeColor(Color.CYAN);
   	  
       	  }
   	  
   	  }
   	
    	// Las lineas
    	 
    	roigrid = new Line(Xlvl[0],0,Xlvl[0],height); 
  	  roigrid.setStrokeColor(Color.red);  // Primer vertical entre 100 y 709
  	  roigrid.setStrokeWidth(1);
  	  
  	  //roigrid.setPosition(2);
  	  overlay.add(roigrid);
  	  
  	  roigrid = new Line(Xlvl[1],0,Xlvl[1],height);
  	  roigrid.setStrokeColor(Color.red); // Segunda Vertical entre 709 y 1234
  	  roigrid.setStrokeWidth(1);
  	  
  	  //roigrid.setPosition(2);
  	  overlay.add(roigrid);

  	  
  	  roigrid = new Line(0,Ylvl[0],width,Ylvl[0]); 
  	  roigrid.setStrokeColor(Color.red);  // Primer Horizontal entre 60 y 456
  	  roigrid.setStrokeWidth(1);
  	  
  	  //roigrid.setPosition(2);
  	  overlay.add(roigrid);
  	  
  	  roigrid = new Line(0,Ylvl[1],width,Ylvl[1]);
  	  roigrid.setStrokeColor(Color.red); // Segunda Horizontal entre 456 y 892
  	  roigrid.setStrokeWidth(1);
  	  
  	  //roigrid.setPosition(2);
  	  overlay.add(roigrid);
  	 
  	  
     //  Los numeros de Z
  	 	  
  	  //1
  		  stack_roi[currentslice][0] = new TextRoi((Inter1[0]*0.1),(Inter2[0]*0.1),0, 0, String.valueOf(roll[currentslice][0]), font); // Listo
  		  stack_roi[currentslice][0].setStrokeColor(Color.YELLOW);
  		  //stack_roi[currentslice][0].setPosition(currentslice+1);
  		  overlay.add(stack_roi[currentslice][0]);		  
	    	  
	  //4  	  
	    	  stack_roi[currentslice][1] = new TextRoi((Inter1[0]*0.1),Ylvl[0]+(Inter2[0]*0.1),0, 0, String.valueOf(roll[currentslice][1]), font); // Listo
	    	  stack_roi[currentslice][1].setStrokeColor(Color.YELLOW);
	    	  //stack_roi[currentslice][1].setPosition(currentslice+1);
	    	  overlay.add(stack_roi[currentslice][1]);    	  
	   //7 	  
	    	  stack_roi[currentslice][2] = new TextRoi((Inter1[0]*0.1),Ylvl[1]+(Inter2[0]*0.1),0,0, String.valueOf(roll[currentslice][2]), font); // Listo
	    	  stack_roi[currentslice][2].setStrokeColor(Color.YELLOW);
	    	  //stack_roi[currentslice][2].setPosition(currentslice+1);
	    	  overlay.add(stack_roi[currentslice][2]);
	   
	  //2  	  
	    	  stack_roi[currentslice][1] = new TextRoi(Xlvl[0]+(Inter1[0]*0.1),(Inter2[0]*0.1),0, 0, String.valueOf(roll[currentslice][3]), font); // Listo
	    	  stack_roi[currentslice][1].setStrokeColor(Color.YELLOW);
	    	  //stack_roi[currentslice][3].setPosition(currentslice+1);
	    	  overlay.add(stack_roi[currentslice][1]);
	  
	  //5  	  
	    	  stack_roi[currentslice][4] = new TextRoi(Xlvl[0]+(Inter1[0]*0.1),Ylvl[0]+(Inter2[0]*0.1),0, 0, String.valueOf(roll[currentslice][4]), font); // Listo
	    	  stack_roi[currentslice][4].setStrokeColor(Color.YELLOW);
	    	  //stack_roi[currentslice][4].setPosition(currentslice+1);
	    	  overlay.add(stack_roi[currentslice][4]);
	   // 8 	  
	    	  stack_roi[currentslice][5] = new TextRoi(Xlvl[0]+(Inter1[0]*0.1),Ylvl[1]+(Inter2[0]*0.1),0, 0,String.valueOf(roll[currentslice][5]), font);
	    	  stack_roi[currentslice][5].setStrokeColor(Color.YELLOW);
	    	  //stack_roi[currentslice][5].setPosition(currentslice+1);
	    	  overlay.add(stack_roi[currentslice][5]);
	    
	   //3 	  
	    	  stack_roi[currentslice][6] = new TextRoi(Xlvl[1]+(Inter1[0]*0.1),(Inter2[0]*0.1),0, 0, String.valueOf(roll[currentslice][6]), font);
	    	  stack_roi[currentslice][6].setStrokeColor(Color.YELLOW);
	    	  //stack_roi[currentslice][6].setPosition(currentslice+1);
	    	  overlay.add(stack_roi[currentslice][6]);
	   
	   //6 	  
	    	  stack_roi[currentslice][7] = new TextRoi(Xlvl[1]+(Inter1[0]*0.1),Ylvl[0]+(Inter2[0]*0.1),0, 0, String.valueOf(roll[currentslice][7]), font);
	    	  stack_roi[currentslice][7].setStrokeColor(Color.YELLOW);
	    	  //stack_roi[currentslice][7].setPosition(currentslice+1);
	    	  overlay.add(stack_roi[currentslice][7]);
	   //9 	  
	    	  stack_roi[currentslice][8] = new TextRoi(Xlvl[1]+(Inter1[0]*0.1),Ylvl[1]+(Inter2[0]*0.1),0, 0, String.valueOf(roll[currentslice][8]), font);
	    	  stack_roi[currentslice][8].setStrokeColor(Color.YELLOW);
	    	  //stack_roi[currentslice][8].setPosition(currentslice+1);
	    	  overlay.add(stack_roi[currentslice][8]);
	    	  
	    	    
  	
	    	  //Stack_Tp.setOverlay(numberoverlay);
	    	  Stack_Tp.setOverlay(overlay);
    	
    	
	}


	////Calculo de interpolacion lineal desde 9 puntos hasta completar el ancho x alto de la imagen


    private static void calcular_borrado_inicial(int [] parser, int parser_count, String choice) 
    {
	
    	
    	float [] vec1, vec2, vec3, vec4, vec5, vec6;
	     
	      //para Y = 60
	      vec1 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec2 = new float [] { lvl_corte,lvl_corte,lvl_corte}; //z 
	      //vec2 = new float [] {8,14,30}; //z 
		  //{100,709,1234}
	      
	       //para Y = 456
	      
	      vec3 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec4 = new float [] {lvl_corte,lvl_corte,lvl_corte}; //z
	      //vec4 = new float [] {21,33,45}; //z
		     
	      
	      // para Y = 892
	      
	      vec5 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec6 = new float [] {lvl_corte,lvl_corte,lvl_corte}; //z
	      //vec6 = new float [] {60,72,72};
	      
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
		 for(int h=0; h < Inter2[1] ; h++)
			{
				vectoryvalue [h]= h; 
  	 	}
		 for(int h=0; h < (height-Inter2[1]) ; h++)
			{
				vectory2value [h]= h; 
  	 	}
		 
	     for(int h=0; h < width ; h++)
			{
	    	 
	    	 float spline = (float) sf1.evalSpline(vec1,vec2,vec1.length,h); //y=0
	    	 	//System.out.println("Y = 0: "+ spline);
	    	 float spline2 = (float) sf2.evalSpline(vec3,vec4,vec3.length,h);//y=3
	    	 	//System.out.println("X = "+ h+"Y = 46: "+ spline2);
	    	 float spline3 = (float) sf3.evalSpline(vec5,vec6,vec5.length,h);//y=3
	    	 	
	    	 	vectorx0y0 [h] =  spline;
	    	 	vectorx0y46 [h] = spline2;
	    	 	vectorx0y89 [h] = spline3;
	    	 
	    	 	//System.out.println("Inicio vector X = "+h+" //: Y=0: z="+ spline+ " - Y=3: z=" +spline2);
	    	 	//Con los parametros de corte de Y =0 e Y=3, 
	    	 	//se pueden obtener las rectas de x1 hasta xn
	    	 	
	    	 	zlabel.putPixelValue(h,Inter2[0],vectorx0y0[h]);
	    	 	zlabel.putPixelValue(h,Inter2[1],vectorx0y46[h]);
	    	 	zlabel.putPixelValue(h,Inter2[2],vectorx0y89[h]);
	    	 	
			}
	      
	     
	     float [] vectorauxl1 = {Inter2[0],Inter2[1]};
	     float [] vectorauxl2 = {Inter2[1],Inter2[2]};
	     
	     float [] vectorspl = new float [width];
				 
		 //System.out.println();

		 //sacar valores para todo X , Y 
		
		 for(int X=0; X < width ; X++)
			{
			 float [] allxvalues = new float [2];
			 allxvalues[0]= vectorx0y0[X];
		     allxvalues[1]= vectorx0y46[X];
		     //System.out.println("X2 :"+allxvalues[0]+ " X2: "+ allxvalues[1]);

			 for(int Y=0; Y < Inter2[1] ; Y++)
					{
				 			
			    		  sf4 = new SplineFitter(vectorauxl1,allxvalues, 2);
					      
			    		  float spline3 = (float) sf4.evalSpline(vectorauxl1,allxvalues,vectorauxl1.length,Y);//y=3
				    	 	//System.out.println("Y = 3: "+ spline2);
				    	 	
			    		  if (X==100 || X==709 || X==1234)
				    	 	{
				    	 		zlabel.putPixelValue(X,60,255);
					    	 	zlabel.putPixelValue(X,456,255);
					    	 	
				    	 	}
				    	 	vectorspl [Y] = spline3;
				    	 	zlabel.putPixelValue(X,Y,vectorspl[Y]);
				    	 	 //System.out.println("Inicio vector Y = "+Y+" // z="+ spline3);
				 		
					}
			}
		 
		  //SEGUNDO LAYER
		 
		 float [] vectorspl2 = new float [width];
			
		 float seclay = (width - Inter2[1]);
		 
		 for(int X=0; X < width ; X++)
			{
			 float [] allx2values = new float [2];
			 allx2values[0]= vectorx0y46[X];
		     allx2values[1]= vectorx0y89[X];
		     if(X==1234)
		     {
		    	 
				 
		     }
	         	 
		   // System.out.println("X2 :"+allx2values[0]+ " X2: "+ allx2values[1]);
			 for(int Y=0; Y < (seclay) ; Y++)
					{
				 		
				         
			    		  sf5 = new SplineFitter(vectorauxl2,allx2values, 2);
					      
			    		  float spline4 = (float) sf5.evalSpline(vectorauxl2,allx2values,vectorauxl2.length,(Y+Inter2[1]));//y=3
				    	  
			    		  
			    		//  System.out.println("Y = 3: "+ spline4);
				    	 		
				    	  
			    		  if (X==100 || X==709 || X==1234)
				    	 	{
				    	 		zlabel.putPixelValue(X,456,255);
					    	 	zlabel.putPixelValue(X,892,255);
					    	 	
				    	 	}
				    	 	vectorspl2 [Y] = spline4;
				    	 	 
				    	 	zlabel.putPixelValue(X,(Y+Inter2[1]),vectorspl2[Y]);
				    	 	 if(X==1234)
						     { 
				    	 	  //System.out.println("Inicio vector Y = "+(Y+Inter2[1])+" // z="+ spline4);
						     }
					}
			}
		 
			 
		    
	    
	    //System.out.println(" X :"+width+ "// Y :" + height);
	    
	   
	    
	    ImageStack stack_tiempos = new ImageStack(width, height); 	
	    
	// se realiza para cada stack seleccionado el corte en la profundidad h    
	    
	    for(int z=0; z < parser_count ; z++)
		{
	    	//System.out.println("Va en el calculo de: "+ parser[z]);
	    	 ImageStack tiempos = new ImageStack(width, height); 	
	  	   
	    	
		    for(int h=1; h <= zs ; h++)
			{
		    	 ImageProcessor nueva = new ByteProcessor(width, height);
				// nueva.setValue(255); // white = 255 
				 //nueva.fill(); 
		    	
				for ( int i = 0; i < width ; i++ )
		    	{
	  		
		    		for ( int j = 0; j < height; j++ )
		    		{
		    			imageprev.setSlice(h+(zs*(parser[z]-1))); //TIEMPO - 1
		    			
		    			float zcorte = zlabel.getPixel(i, j);
		    			int[] actualz = imageprev.getPixel(i, j);
		    		
		    			nueva.putPixelValue(i,j, actualz[0]);
		    			
		    				    			
		    			if (choice.equals(">"))
		    			{
		    				if (zcorte >= h	)
			    			{
			    			 nueva.putPixelValue(i, j, 0);
			    			}
		    			}
		    			
		    			if (choice.equals("<"))
		    			{
		    				if (zcorte <= h	)
			    			{
			    			 nueva.putPixelValue(i, j, 0);
			    			}
		    			}
		    			
		    			
		    		}
		    	}
		    	tiempos.addSlice(nueva);
		    	
			}	
		
		    ImagePlus Tp = new ImagePlus("Stack por tiempos seleccionados", tiempos);
		    
		    //System.out.println("Imagen con Z");
		    
		    //Tp.show();
		    
		    // Proyeccion para cada tiempo
		    
		    
		   ZProjector projectar_t = new ZProjector(Tp); 
			
			projectar_t.setMethod(ZProjector.MAX_METHOD);
			projectar_t.setStartSlice(1 + (1-1)*zs);
			projectar_t.setStopSlice(1*zs);
			projectar_t.doProjection();
			
			ImagePlus projection_t = projectar_t.getProjection();
			//sprojection_t.show();
			
			stack_tiempos.addSlice(projection_t.getProcessor());
			
		    	
		}
	    
	    Stack_Tp = new ImagePlus("Max_Stack_all", stack_tiempos);
	 
   	 
	    
		Stack_Tp.show();
		
		//IJ.run("Tile");
	}

//////////////////// MODIFICAR PREVIE TIEMPOS CON SCROLL , ///////////////////////
////////////////////    lo mismo que el anterior pero se llama cada vez que hay un cambio   ////////////////
    
    private static void calcular_borrado_scroll (int [] parser, int parser_count, String choice) 
    {
    	
    	
    	float [] vec1, vec2, vec3, vec4, vec5, vec6;
	     
	      //para Y = 60
	      vec1 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec2 = new float [] {(float) roll[currentslice][0],(float) roll[currentslice][3],(float) roll[currentslice][6]}; //z 
	      //vec2 = new float [] {8,14,30}; //z 
		  //{100,709,1234}
	      
	       //para Y = 456
	      
	      vec3 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec4 = new float [] {(float) roll[currentslice][1],(float) roll[currentslice][4],(float) roll[currentslice][7]}; //z
	      //vec4 = new float [] {21,33,45}; //z
		     
	      
	      // para Y = 892
	      
	      vec5 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec6 = new float [] {(float) roll[currentslice][2],(float) roll[currentslice][5],(float) roll[currentslice][8]}; //z
	      //vec6 = new float [] {60,72,72};
	      
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
		 for(int h=0; h < Inter2[1] ; h++)
			{
				vectoryvalue [h]= h; 
	 	}
		 for(int h=0; h < (height-Inter2[1]) ; h++)
			{
				vectory2value [h]= h; 
	 	}
		 
	     for(int h=0; h < width ; h++)
			{
	    	 
	    	 float spline = (float) sf1.evalSpline(vec1,vec2,vec1.length,h); //y=0
	    	 	//System.out.println("Y = 0: "+ spline);
	    	 float spline2 = (float) sf2.evalSpline(vec3,vec4,vec3.length,h);//y=3
	    	 	//System.out.println("X = "+ h+"Y = 46: "+ spline2);
	    	 float spline3 = (float) sf3.evalSpline(vec5,vec6,vec5.length,h);//y=3
	    	 	
	    	 	vectorx0y0 [h] =  spline;
	    	 	vectorx0y46 [h] = spline2;
	    	 	vectorx0y89 [h] = spline3;
	    	 
	    	 	//System.out.println("Inicio vector X = "+h+" //: Y=0: z="+ spline+ " - Y=3: z=" +spline2);
	    	 	//Con los parametros de corte de Y =0 e Y=3, 
	    	 	//se pueden obtener las rectas de x1 hasta xn
	    	 	
	    	 	zlabel.putPixelValue(h,Inter2[0],vectorx0y0[h]);
	    	 	zlabel.putPixelValue(h,Inter2[1],vectorx0y46[h]);
	    	 	zlabel.putPixelValue(h,Inter2[2],vectorx0y89[h]);
	    	 	
			}
	      
	     
	     float [] vectorauxl1 = {Inter2[0],Inter2[1]};
	     float [] vectorauxl2 = {Inter2[1],Inter2[2]};
	     
	     float [] vectorspl = new float [width];
				 
		 //System.out.println();

		 //sacar valores para todo X , Y 
		
		 for(int X=0; X < width ; X++)
			{
			 float [] allxvalues = new float [2];
			 allxvalues[0]= vectorx0y0[X];
		     allxvalues[1]= vectorx0y46[X];
		     //System.out.println("X2 :"+allxvalues[0]+ " X2: "+ allxvalues[1]);

			 for(int Y=0; Y < Inter2[1] ; Y++)
					{
				 			
			    		  sf4 = new SplineFitter(vectorauxl1,allxvalues, 2);
					      
			    		  float spline3 = (float) sf4.evalSpline(vectorauxl1,allxvalues,vectorauxl1.length,Y);//y=3
				    	 	//System.out.println("Y = 3: "+ spline2);
				    	 	
			    		  if (X==100 || X==709 || X==1234)
				    	 	{
				    	 		zlabel.putPixelValue(X,60,255);
					    	 	zlabel.putPixelValue(X,456,255);
					    	 	
				    	 	}
				    	 	vectorspl [Y] = spline3;
				    	 	zlabel.putPixelValue(X,Y,vectorspl[Y]);
				    	 	 //System.out.println("Inicio vector Y = "+Y+" // z="+ spline3);
				 		
					}
			}
		 
		  //SEGUNDO LAYER
		 
		 float [] vectorspl2 = new float [width];
			
		 float seclay = (width - Inter2[1]);
		 
		 for(int X=0; X < width ; X++)
			{
			 float [] allx2values = new float [2];
			 allx2values[0]= vectorx0y46[X];
		     allx2values[1]= vectorx0y89[X];
		    
		     
		     for(int Y=0; Y < (seclay) ; Y++)
					{
				 		
				         
			    		  sf5 = new SplineFitter(vectorauxl2,allx2values, 2);
					      
			    		  float spline4 = (float) sf5.evalSpline(vectorauxl2,allx2values,vectorauxl2.length,(Y+Inter2[1]));//y=3
				    	  
			    		  
			    		//  System.out.println("Y = 3: "+ spline4);
				    	 		
				    	  
			    		  if (X==100 || X==709 || X==1234)
				    	 	{
				    	 		zlabel.putPixelValue(X,456,255);
					    	 	zlabel.putPixelValue(X,892,255);
					    	 	
				    	 	}
				    	 	vectorspl2 [Y] = spline4;
				    	 	 
				    	 	zlabel.putPixelValue(X,(Y+Inter2[1]),vectorspl2[Y]);
				    	 	 if(X==1234)
						     { 
				    	 	  //System.out.println("Inicio vector Y = "+(Y+Inter2[1])+" // z="+ spline4);
						     }
					}
			}

		 ImageStack tiempos = new ImageStack(width, height); 	
	  	   
	    	
		    for(int h=1; h <= zs ; h++)
			{
		    	 ImageProcessor nueva = new ByteProcessor(width, height);
				// nueva.setValue(255); // white = 255 
				 //nueva.fill(); 
		    	
				for ( int i = 0; i < width ; i++ )
		    	{
	  		
		    		for ( int j = 0; j < height; j++ )
		    		{
		    			imageprev.setSlice(h+(zs*(parser[currentslice]-1))); //TIEMPO - 1
		    			
		    			float zcorte = zlabel.getPixel(i, j);
		    			int[] actualz = imageprev.getPixel(i, j);
		    		
		    			nueva.putPixelValue(i,j, actualz[0]);
		    			
		    				    			
		    			if (choice.equals(">"))
		    			{
		    				if (zcorte >= h	)
			    			{
			    			 nueva.putPixelValue(i, j, 0);
			    			}
		    			}
		    			
		    			if (choice.equals("<"))
		    			{
		    				if (zcorte <= h	)
			    			{
			    			 nueva.putPixelValue(i, j, 0);
			    			}
		    			}
		    			
		    			
		    		}
		    	}
		    	tiempos.addSlice(nueva);
		    	
			}	
		
		    ImagePlus Tp = new ImagePlus("Stack por tiempos seleccionados", tiempos);
		    
		    //System.out.println("Imagen con Z");
		    
		  //  Tp.show();
		    
		   ZProjector projectar_t = new ZProjector(Tp); 
			
			projectar_t.setMethod(ZProjector.MAX_METHOD);
			projectar_t.setStartSlice(1 + (1-1)*zs);
			projectar_t.setStopSlice(1*zs);
			projectar_t.doProjection();
			
			ImagePlus projection_t = projectar_t.getProjection();
			
			//IJ.run("Tile");

		
    	
    	for ( int x=0; x<width;x++)
    	{
    		for ( int y=0; y<height;y++)
    		{
    			//Stack_Tp.setSlice(currentslice);
    			
    			int[] pixel_update_scroll = projection_t.getPixel(x,y);
    			
    			Stack_Tp.getProcessor().putPixelValue(x,y,(pixel_update_scroll[0]));
    		
    			//Stack_Tp.setSlice(currentslice);
    		}
    	}
    	
    	robot.delay(1);
		robot.mousePress(MouseEvent.BUTTON1_MASK);
		robot.mouseRelease(MouseEvent.BUTTON1_MASK);
  
    	
	}
    
    
    /// Misma funcion, considerando que no se seleccionaron tiempos antes.

    private static void calcular_borrado_totalsintiempo(int [] parser, int parser_count, String choice) 
    {
	
      
	    ImageStack stack_tiempos = new ImageStack(width, height); 	
	    
		    
	    for(int z=0; z < tiempos ; z++)
		{
	    	//System.out.println("Va en el calculo de: "+ parser[z]);
	    	 ImageStack tiempos = new ImageStack(width, height); 	
	  	   
	    	
		    for(int h=1; h <= zs ; h++)
			{
		    	 ImageProcessor nueva = new ByteProcessor(width, height);
				// nueva.setValue(255); // white = 255 
				 //nueva.fill(); 
		    	
				for ( int i = 0; i < width ; i++ )
		    	{
	  		
		    		for ( int j = 0; j < height; j++ )
		    		{
		    			imageprev.setSlice(h+(zs*z)); //TIEMPO - 1
		    			
		    			float zcorte = lvl_corte;
		    			int[] actualz = imageprev.getPixel(i, j);
		    		
		    			nueva.putPixelValue(i,j, actualz[0]);
		    			
		    				    			
		    			if (choice.equals(">"))
		    			{
		    				if (zcorte >= h	)
			    			{
			    			 nueva.putPixelValue(i, j, 0);
			    			}
		    			}
		    			
		    			if (choice.equals("<"))
		    			{
		    				if (zcorte <= h	)
			    			{
			    			 nueva.putPixelValue(i, j, 0);
			    			}
		    			}
		    			
		    			
		    		}
		    	}
		    	tiempos.addSlice(nueva);
		    	
			}	
		
		    
		    
		    
		    
		    ImagePlus Tp = new ImagePlus("Stack por tiempos seleccionados", tiempos);
		    
		    //System.out.println("Imagen con Z");
		    
		    //Tp.show();
		    
		   ZProjector projectar_t = new ZProjector(Tp); 
			
			projectar_t.setMethod(ZProjector.MAX_METHOD);
			projectar_t.setStartSlice(1 + (1-1)*zs);
			projectar_t.setStopSlice(1*zs);
			projectar_t.doProjection();
			
			ImagePlus projection_t = projectar_t.getProjection();
			//sprojection_t.show();
			
			stack_tiempos.addSlice(projection_t.getProcessor());
			
		    	
		}
	    
	    Stack_Tp = new ImagePlus("Max_Stack_all", stack_tiempos);
	 
   	 
	    
	    Stack_Tp.show();
		
		//IJ.run("Tile");
	}
    
    
////////////////////  Calculo en caso de seleccionar tiempos y editar estos ///////////////////

    private static void calcular_borrado_total (int [] parser, int parser_count, String choice) 
    {
    	
    	
     	ImageStack zlabel_stack = new ImageStack(width, height);
    	
    	float [] vec1_all_time, vec2_all_time;
    	
    	for (int x = 0; x < parser_count; x++)
    	{
    		
    	
    	
    	float [] vec1, vec2, vec3, vec4, vec5, vec6;
	     
	      //para Y = 60
	      vec1 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec2 = new float [] {(float) roll[x][0],(float) roll[x][3],(float) roll[x][6]}; //z 
	      //vec2 = new float [] {8,14,30}; //z 
		  //{100,709,1234}
	      
	       //para Y = 456
	      
	      vec3 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec4 = new float [] {(float) roll[x][1],(float) roll[x][4],(float) roll[x][7]}; //z
	      //vec4 = new float [] {21,33,45}; //z
		     
	      
	      // para Y = 892
	      
	      vec5 = new float [] {Inter1[0],Inter1[1],Inter1[2]}; //x
	      vec6 = new float [] {(float) roll[x][2],(float) roll[x][5],(float) roll[x][8]}; //z
	      //vec6 = new float [] {60,72,72};
	      
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
		 for(int h=0; h < Inter2[1] ; h++)
			{
				vectoryvalue [h]= h; 
	 	}
		 for(int h=0; h < (height-Inter2[1]) ; h++)
			{
				vectory2value [h]= h; 
	 	}
		 
	     for(int h=0; h < width ; h++)
			{
	    	 
	    	 float spline = (float) sf1.evalSpline(vec1,vec2,vec1.length,h); //y=0
	    	 	//System.out.println("Y = 0: "+ spline);
	    	 float spline2 = (float) sf2.evalSpline(vec3,vec4,vec3.length,h);//y=3
	    	 	//System.out.println("X = "+ h+"Y = 46: "+ spline2);
	    	 float spline3 = (float) sf3.evalSpline(vec5,vec6,vec5.length,h);//y=3
	    	 	
	    	 	vectorx0y0 [h] =  spline;
	    	 	vectorx0y46 [h] = spline2;
	    	 	vectorx0y89 [h] = spline3;
	    	 
	    	 	//System.out.println("Inicio vector X = "+h+" //: Y=0: z="+ spline+ " - Y=3: z=" +spline2);
	    	 	//Con los parametros de corte de Y =0 e Y=3, 
	    	 	//se pueden obtener las rectas de x1 hasta xn
	    	 	
	    	 	
	    	 	
	    	 	
	    	 	zlabel_times.putPixelValue(h,Inter2[0],vectorx0y0[h]);
	    	 	zlabel_times.putPixelValue(h,Inter2[1],vectorx0y46[h]);
	    	 	zlabel_times.putPixelValue(h,Inter2[2],vectorx0y89[h]);
	    	 	
			}
	      
	     
	     float [] vectorauxl1 = {Inter2[0],Inter2[1]};
	     float [] vectorauxl2 = {Inter2[1],Inter2[2]};
	     
	     float [] vectorspl = new float [width];
				 
		 //System.out.println();

		 //sacar valores para todo X , Y 
		
		 for(int X=0; X < width ; X++)
			{
			 float [] allxvalues = new float [2];
			 allxvalues[0]= vectorx0y0[X];
		     allxvalues[1]= vectorx0y46[X];
		     //System.out.println("X2 :"+allxvalues[0]+ " X2: "+ allxvalues[1]);

			 for(int Y=0; Y < Inter2[1] ; Y++)
					{
				 			
			    		  sf4 = new SplineFitter(vectorauxl1,allxvalues, 2);
					      
			    		  float spline3 = (float) sf4.evalSpline(vectorauxl1,allxvalues,vectorauxl1.length,Y);//y=3
				    	 	//System.out.println("Y = 3: "+ spline2);
				    	 	
			    		  if (X==100 || X==709 || X==1234)
				    	 	{
				    	 		zlabel_times.putPixelValue(X,60,255);
					    	 	zlabel_times.putPixelValue(X,456,255);
					    	 	
				    	 	}
				    	 	vectorspl [Y] = spline3;
				    	 	zlabel_times.putPixelValue(X,Y,vectorspl[Y]);
				    	 	 //System.out.println("Inicio vector Y = "+Y+" // z="+ spline3);
				 		
					}
			}
		 
		  //SEGUNDO LAYER
		 
		 float [] vectorspl2 = new float [width];
			
		 float seclay = (width - Inter2[1]);
		 
		 for(int X=0; X < width ; X++)
			{
			 float [] allx2values = new float [2];
			 allx2values[0]= vectorx0y46[X];
		     allx2values[1]= vectorx0y89[X];
		    
		     
		     for(int Y=0; Y < (seclay) ; Y++)
					{
				 		
				         
			    		  sf5 = new SplineFitter(vectorauxl2,allx2values, 2);
					      
			    		  float spline4 = (float) sf5.evalSpline(vectorauxl2,allx2values,vectorauxl2.length,(Y+Inter2[1]));//y=3
				    	  
			    		  
			    		//  System.out.println("Y = 3: "+ spline4);
				    	 		
				    	  
			    		  if (X==100 || X==709 || X==1234)
				    	 	{
				    	 		zlabel_times.putPixelValue(X,456,255);
					    	 	zlabel_times.putPixelValue(X,892,255);
					    	 	
				    	 	}
				    	 	vectorspl2 [Y] = spline4;
				    	 	 
				    	 	zlabel_times.putPixelValue(X,(Y+Inter2[1]),vectorspl2[Y]);
				    	 	 if(X==1234)
						     { 
				    	 	  //System.out.println("Inicio vector Y = "+(Y+Inter2[1])+" // z="+ spline4);
						     }
					}
			}
	     
		 zlabel_stack.addSlice(zlabel_times);
		 //////////////////////////// FIN DEL CICLO POR TIEMPO
    	}
		 
    	ImagePlus todo_label = new ImagePlus ("asdasd",zlabel_stack);
    	
	  //	todo_label.show();
	    
    	ImageStack all_label = new ImageStack(width, height);
	     
	     for(int r = 0; r < tiempos; r++ )
	     {

		     ImageProcessor every_label = new ByteProcessor(width, height);   
		     every_label.setValue(255); // white = 255 
		     every_label.fill();
		     
		     all_label.addSlice(every_label);
		     
	     }
	     
	    ImagePlus final_all = new ImagePlus ("Max_Stack_all",all_label);
	    
	    //final_all.show();
    	
    	//zlabel_stack.addSlice(zlabel_times);
    	
     	vec1_all_time = new float [parser_count];
    	vec2_all_time = new float [parser_count];
    	
    	for (int x = 0; x < parser_count; x++)
    	{
    		
    		vec1_all_time[x]= parser[x];
    	}
    	
    	for ( int x=0; x<width;x++) //width
    	{
    		for ( int y=0; y<height;y++) //height
    		{
    			
    			for (int f = 1; f <= parser_count; f++)
    	    	{
    				
    				todo_label.setSlice(f);
					vec2_all_time [f-1] =  (float) todo_label.getProcessor().getPixelValue(x, y);
					//vec2_all_time [] =  (float) todo_label.getProcessor().getPixelValue(100, 100);
    	    	}	
					//System.out.println("Valor del Pixel en la slice "+f +" :"+todo_label.getProcessor().getPixelValue(x, y));
    	
    				SplineFitter fitperpixel;
    				
    				fitperpixel = new SplineFitter(vec1_all_time, vec2_all_time, parser_count);
    			   
    			    for(int h=1; h <= tiempos ; h++) // tiempos
					{
					
					   final_all.setSlice(h);
					   
							   
    			    	float spline_bypixel = (float) fitperpixel.evalSpline(vec1_all_time,vec2_all_time,vec1_all_time.length,h); //y=0
	    			    
    			    	if (spline_bypixel <= 0)
    			    	{
    			    		final_all.getProcessor().putPixelValue(x,y, 1);
    			    	}
    			    	else 
    			    	{
    			    		if(spline_bypixel >= zs)
    			    		{
    			    			final_all.getProcessor().putPixelValue(x,y, zs);
    			    		}
    			    		else
    			    		{
        			    		final_all.getProcessor().putPixelValue(x,y, spline_bypixel);
    			    		}
    			    	}
	    			
    			    	//System.out.println("Spline por tiempo "+ h +" Valor> "+spline_bypixel);
    			    	
	    			}
    				
    				
    	    	
    			
    		}
    		
    	}
    	
    	//// ACA EMPIEZAN EL BORRADO DE TODAS // Z 
    	
    	
   	 ImageStack todos_tiempos = new ImageStack(width, height); 	

    	
    	
    	for(int z=0; z < tiempos ; z++)
		{
	    	//System.out.println("Va en el calculo de: "+ parser[z]);
	    	 ImageStack tiempos = new ImageStack(width, height); 	
	  	   
	    	
		    for(int h=1; h <= zs ; h++)
			{
		    	 ImageProcessor nueva = new ByteProcessor(width, height);
				// nueva.setValue(255); // white = 255 
				 //nueva.fill(); 
		    	
				for ( int i = 0; i < width ; i++ )
		    	{
	  		
		    		for ( int j = 0; j < height; j++ )
		    		{
		    			imageprev.setSlice(h+(zs*z)); //TIEMPO - 1
		    			final_all.setSlice(z+1);
		    			
		    			int[] zcorte = final_all.getPixel(i, j);
		    			int[] actualz = imageprev.getPixel(i, j);
		    		
		    			nueva.putPixelValue(i,j, actualz[0]);
		    			
		    				    			
		    			if (choice.equals(">"))
		    			{
		    				if (zcorte[0] >= h	)
			    			{
			    			 nueva.putPixelValue(i, j, 0);
			    			}
		    			}
		    			
		    			if (choice.equals("<"))
		    			{
		    				if (zcorte[0] <= h	)
			    			{
			    			 nueva.putPixelValue(i, j, 0);
			    			}
		    			}
		    			
		    			
		    		}
		    	}
		    	tiempos.addSlice(nueva);
		    	
			}	
		
		    
		    
		    
		    
		    ImagePlus Tp = new ImagePlus("Stack por tiempos seleccionados", tiempos);
		    
		    //System.out.println("Imagen con Z");
		    
		    //Tp.show();
		    
		   ZProjector projectar_t = new ZProjector(Tp); 
			
			projectar_t.setMethod(ZProjector.MAX_METHOD);
			projectar_t.setStartSlice(1 + (1-1)*zs);
			projectar_t.setStopSlice(1*zs);
			projectar_t.doProjection();
			
			ImagePlus projection_t = projectar_t.getProjection();
			//sprojection_t.show();
			
			todos_tiempos.addSlice(projection_t.getProcessor());
			
		    	
		}
    	
    	ImagePlus All = new ImagePlus("Imagen Stack Tiempos", todos_tiempos);
	    
    	
    	
    	All.show();
    	//final_all.show();

    }
    
    
	private static void ShowWinCanvas() {
		// TODO Auto-generated method stub
		
	}

	
	/// Lo que permite la interaccion del cursor y las capas es esta clase

	static class Window extends StackWindow implements MouseWheelListener { 
        /**
	 * 
	 */
	private static final long serialVersionUID = -9129553456411726769L;
		//private ImagePlus imageprev2;
		
		

		public Window(ImagePlus Stack_Tp, ImageCanvas canvas) { 
                super(Stack_Tp, canvas); 
                addMouseWheelListener(this); 
                //gd.addMouseWheelListener(this);
            //     imageprev2 = IJ.openVirtual(direc + imagedir);
              
             	
         		 
                 
        } 

        public void mouseWheelMoved(MouseWheelEvent event) { 
                synchronized(this) { 
                	
                	
                	if (getCanvas().cursorOverImage())
            		{   
                	    int zoom = event.getWheelRotation(); 
                        String cadena = "";
                        
                    		System.out.println("Esta en la SLice :" +Stack_Tp.getCurrentSlice());  
                        	// System.out.println("Valor del cuadro obvio"+ gcs);

                        
                    
            		Point point = getCanvas().getMousePosition();
            		double magnificacion = (double) getCanvas().getMagnification();
            		
            		
            			int Xcursor = (int) (((point.getX() * 1)) / magnificacion);
            			int Ycursor = (int) (((point.getY() * 1)) / magnificacion);
            			

//////////////////////////////////////////////////////////////////////////////
                       // PUNTO 1//
	
            			if (Xcursor>=0 && Xcursor<= Xlvl[0] && Ycursor>=0 && Ycursor<= Ylvl[0] && crtlpress)
            			{
            				
            			    if (zoom<0)  
	                        {	
            			    	
            			    	int intcadena = (int) (roll[currentslice][0]);
	                            cadena = String.valueOf(intcadena);	
	                            
	                           
	                            
	                        	if (intcadena< zs)
	                        	{
	                        		//System.out.println("Int cadena " + intcadena);
	                        		roll[currentslice][0] +=1;
		                        	//System.out.println("Valor slice"+ (currentslice - 2)+" roll: "+ roll[(currentslice-2)][0]);	
		                        	//System.out.println("Valor slice"+ (currentslice - 1)+" roll: "+ roll[(currentslice-1)][0]);	
		                        	//System.out.println("Valor slice"+ (currentslice)+" roll: "+ roll[currentslice][0]);	

	                        	}
	                        	
	                        	
	                        	robot.delay(1);
	                			robot.mousePress(MouseEvent.BUTTON1_MASK);
	                			robot.mouseRelease(MouseEvent.BUTTON1_MASK);
	                			
	                		    overlay.clear();
	                			llenar_overlay();
	                			
	                        }
	                        else 
	                        {
            			    	
            			    	int intcadena = (int) (roll[currentslice][0]);

	                        	cadena = String.valueOf(intcadena);	
	                        	 if (intcadena>1)
	                        		{
	                        			//System.out.println("Int cadena " + intcadena);
	                        			roll[currentslice][0] -=1;
	                        		}
	                        	robot.delay(1);
	                			robot.mousePress(MouseEvent.BUTTON1_MASK);
	                			robot.mouseRelease(MouseEvent.BUTTON1_MASK);
	                			
	                			//stack_roi[Stack_Tp.getCurrentSlice()][0].setPosition(Stack_Tp.getCurrentSlice());
	                			
	                			overlay.clear();
	                			llenar_overlay();
	                			//stack_roi[Stack_Tp.getCurrentSlice()][0].setPosition(Stack_Tp.getCurrentSlice());
	                			
	                			//overlay.add(stack_roi[Stack_Tp.getCurrentSlice()][0]);
	                        }
	                     
            			} 		
                           
						/////////////////////////////////////////////////
						// PUNTO 2//
						
						if (Xcursor>=Xlvl[0] && Xcursor<= Xlvl[1] && Ycursor>=0 && Ycursor<= Ylvl[0] && crtlpress)
						{
							    if (zoom<0)  
						    {	
							    	int intcadena = (int) (roll[currentslice][3]);
						        cadena = String.valueOf(intcadena);	
						    	if (intcadena< zs)
						    	{
						    		//System.out.println("Int cadena " + intcadena);
						    		roll[currentslice][3] +=1;
						    	}
						    	
						    	//System.out.println("Valor de zoom mas"+wheel);
						       	
						    	robot.delay(1);
								robot.mousePress(MouseEvent.BUTTON1_MASK);
								robot.mouseRelease(MouseEvent.BUTTON1_MASK);
								
								overlay.clear();
	                			llenar_overlay();
	                			
						    }
						    else 
						    {
						    	//System.out.println("Valor de zoom menos"+wheel);
						    		
						    	
						    	int intcadena = (int) (roll[currentslice][3]);
						        cadena = String.valueOf(intcadena);	
						    	    if (intcadena>1)
						    		{
						    			//System.out.println("Int cadena " + intcadena);
						    			 roll[currentslice][3] -=1;
						    		}
						    	robot.delay(1);
								robot.mousePress(MouseEvent.BUTTON1_MASK);
								robot.mouseRelease(MouseEvent.BUTTON1_MASK);
								
								overlay.clear();
	                			llenar_overlay();
								
						    }
						  
						}
			
						/////////////////////////////////////////////////
						// PUNTO 3//
						
						if (Xcursor>=Xlvl[1] && Xcursor<= width && Ycursor>=0 && Ycursor<= Ylvl[0] && crtlpress)
						{
							if (zoom<0)  
							{	
								int intcadena = (int) (roll[currentslice][6]);
								cadena = String.valueOf(intcadena);	
								
								if (intcadena< zs)
								{
									//System.out.println("Int cadena " + intcadena);
									roll[currentslice][6] +=1;
								}
						
								//System.out.println("Valor de zoom mas"+wheel);
								
								robot.delay(1);
								robot.mousePress(MouseEvent.BUTTON1_MASK);
								robot.mouseRelease(MouseEvent.BUTTON1_MASK);
								
								overlay.clear();
	                			llenar_overlay();
								
						}
						else 
						{
							//System.out.println("Valor de zoom menos"+wheel);
								
							
							int intcadena = (int) (roll[currentslice][6]);
							cadena = String.valueOf(intcadena);	
							    if (intcadena>1)
								{
									//System.out.println("Int cadena " + intcadena);
									 roll[currentslice][6] -=1;
								}
							robot.delay(1);
							robot.mousePress(MouseEvent.BUTTON1_MASK);
							robot.mouseRelease(MouseEvent.BUTTON1_MASK);
							
							overlay.clear();
                			llenar_overlay();
							
							
							
						}
						
						}

            			
            			   /////////////////////////////////////////////////
                           // PUNTO 4//

            			
            			if (Xcursor>=0 && Xcursor<= Xlvl[0] && Ycursor>=Ylvl[0] && Ycursor<= Ylvl[1] && crtlpress)
            			{
               			    if (zoom<0)  
	                        {	
               			    	int intcadena = (int) (roll[currentslice][1]);
	                            cadena = String.valueOf(intcadena);	
	                        	if (intcadena< zs)
	                        	{
	                        		//System.out.println("Int cadena " + intcadena);
	                        		roll[currentslice][1] +=1;
	                        	}
	                        	
	                        	//System.out.println("Valor de zoom mas"+wheel);
	                           	robot.delay(1);
	                			robot.mousePress(MouseEvent.BUTTON1_MASK);
	                			robot.mouseRelease(MouseEvent.BUTTON1_MASK);
	                			
	                			overlay.clear();
	                			llenar_overlay();
								
	                        }
	                        else 
	                        {
	                        	//System.out.println("Valor de zoom menos"+wheel);
	                        		
	                        	
	                        	int intcadena = (int) (roll[currentslice][1]);
	                            cadena = String.valueOf(intcadena);	
	                        	    if (intcadena>1)
	                        		{
	                        			//System.out.println("Int cadena " + intcadena);
	                        			 roll[currentslice][1] -=1;
	                        		}
	                        	robot.delay(1);
	                			robot.mousePress(MouseEvent.BUTTON1_MASK);
	                			robot.mouseRelease(MouseEvent.BUTTON1_MASK);
	                			
	                			overlay.clear();
	                			llenar_overlay();
								
	                			
	                        }
	                      
						            			}
						            			
						/////////////////////////////////////////////////
						// PUNTO 5//
						
						if (Xcursor>=Xlvl[0] && Xcursor<= Xlvl[1] && Ycursor>=Ylvl[0] && Ycursor<= Ylvl[1] && crtlpress)
						{
							if (zoom<0)  
							{	
								int intcadena = (int) (roll[currentslice][4]);
								cadena = String.valueOf(intcadena);	
								
								if (intcadena< zs)
								{
									//System.out.println("Int cadena " + intcadena);
									roll[currentslice][4] +=1;
								}
						
								//System.out.println("Valor de zoom mas"+wheel);
								robot.delay(1);
								robot.mousePress(MouseEvent.BUTTON1_MASK);
								robot.mouseRelease(MouseEvent.BUTTON1_MASK);
								
								overlay.clear();
	                			llenar_overlay();
								
								
						}
						else 
						{
						//System.out.println("Valor de zoom menos"+wheel);
							
						
						int intcadena = (int) (roll[currentslice][4]);
						cadena = String.valueOf(intcadena);	
						    if (intcadena>1)
							{
								//System.out.println("Int cadena " + intcadena);
								 roll[currentslice][4] -=1;
							}
						robot.delay(1);
						robot.mousePress(MouseEvent.BUTTON1_MASK);
						robot.mouseRelease(MouseEvent.BUTTON1_MASK);
						
						overlay.clear();
            			llenar_overlay();
						
						}
						
							
						}
          		
						/////////////////////////////////////////////////
						// PUNTO 6//
						
						if (Xcursor>=Xlvl[1] && Xcursor<= width && Ycursor>=Ylvl[0] && Ycursor<= Ylvl[1] && crtlpress)
						{
							if (zoom<0)  
							{	
								int intcadena = (int) (roll[currentslice][7]);
								cadena = String.valueOf(intcadena);	
								
								if (intcadena< zs)
								{
									//System.out.println("Int cadena " + intcadena);
									roll[currentslice][7] +=1;
								}
						
								//System.out.println("Valor de zoom mas"+wheel);
								robot.delay(1);
								robot.mousePress(MouseEvent.BUTTON1_MASK);
								robot.mouseRelease(MouseEvent.BUTTON1_MASK);
								
								overlay.clear();
	                			llenar_overlay();
								
						
						}
						else 
						{
						//System.out.println("Valor de zoom menos"+wheel);
							
						
						int intcadena = (int) (roll[currentslice][7]);
						cadena = String.valueOf(intcadena);	
						    if (intcadena>1)
							{
								//System.out.println("Int cadena " + intcadena);
								 roll[currentslice][7] -=1;
							}
							robot.delay(1);
							robot.mousePress(MouseEvent.BUTTON1_MASK);
							robot.mouseRelease(MouseEvent.BUTTON1_MASK);
							
							overlay.clear();
                			llenar_overlay();
							
							}
						
						}
						
						/////////////////////////////////////////////////
            			// PUNTO 7//
            			
            			if (Xcursor>=0 && Xcursor<= Xlvl[0] && Ycursor>=Ylvl[1] && Ycursor<= height && crtlpress)
            			{
            				//System.out.println("EL CUADRO ESTE");
               			    if (zoom<0)  
	                        {	
               			    	int intcadena = (int) (roll[currentslice][2]);
	                            cadena = String.valueOf(intcadena);	
	                        	if (intcadena< zs)
	                        	{
	                        		//System.out.println("Int cadena " + intcadena);
	                        		roll[currentslice][2] +=1;
	                        	}
	                        	
	                        	//System.out.println("Valor de zoom mas"+wheel);
	                           	robot.delay(1);
	                			robot.mousePress(MouseEvent.BUTTON1_MASK);
	                			robot.mouseRelease(MouseEvent.BUTTON1_MASK);
	                			
	                			overlay.clear();
	                			llenar_overlay();
								
	                        }
	                        else 
	                        {
	                        	//System.out.println("Valor de zoom menos"+wheel);
	                        		
	                        	
	                        	int intcadena = (int) (roll[currentslice][2]);
	                            cadena = String.valueOf(intcadena);	
	                        	    if (intcadena>1)
	                        		{
	                        			//System.out.println("Int cadena " + intcadena);
	                        			 roll[currentslice][2] -=1;
	                        		}
	                        	robot.delay(1);
	                			robot.mousePress(MouseEvent.BUTTON1_MASK);
	                			robot.mouseRelease(MouseEvent.BUTTON1_MASK);

	                			overlay.clear();
	                			llenar_overlay();
									                			
	                        }
	                      
            			}
						            	    
						/////////////////////////////////////////////////
						// PUNTO 8//
						
						if (Xcursor>=Xlvl[0] && Xcursor<= Xlvl[1] && Ycursor>=Ylvl[1] && Ycursor<= height && crtlpress)
						{
							//System.out.println("EL CUADRO ESTE 8");
							    if (zoom<0)  
						    {	
							    	int intcadena = (int) (roll[currentslice][5]);
						        cadena = String.valueOf(intcadena);	
						    	if (intcadena< zs)
						    	{
						    		//System.out.println("Int cadena " + intcadena);
						    		roll[currentslice][5] +=1;
						    	}
						    	
						    	//System.out.println("Valor de zoom mas"+wheel);
						       	robot.delay(1);
								robot.mousePress(MouseEvent.BUTTON1_MASK);
								robot.mouseRelease(MouseEvent.BUTTON1_MASK);
								
								overlay.clear();
	                			llenar_overlay();
														
						    }
						    else 
						    {
						    	//System.out.println("Valor de zoom menos"+wheel);
						    		
						    	
						    	int intcadena = (int) (roll[currentslice][5]);
						        cadena = String.valueOf(intcadena);	
						    	    if (intcadena>1)
						    		{
						    			//System.out.println("Int cadena " + intcadena);
						    			 roll[currentslice][5] -=1;
						    		}
						    	robot.delay(1);
								robot.mousePress(MouseEvent.BUTTON1_MASK);
								robot.mouseRelease(MouseEvent.BUTTON1_MASK);
								
								overlay.clear();
	                			llenar_overlay();
								
						    }
						  
						}

					/////////////////////////////////////////////////
					// PUNTO 9//
					
					if (Xcursor>=Xlvl[1] && Xcursor<= width && Ycursor>=Ylvl[1] && Ycursor<= height && crtlpress)
					{
						//System.out.println("EL CUADRO ESTE 8");
						    if (zoom<0)  
					    {	
						    	int intcadena = (int) (roll[currentslice][8]);
					        cadena = String.valueOf(intcadena);	
					    	if (intcadena< zs)
					    	{
					    		//System.out.println("Int cadena " + intcadena);
					    		roll[currentslice][8] +=1;
					    	}
					    	
					    	//System.out.println("Valor de zoom mas"+wheel);
					       	robot.delay(1);
							robot.mousePress(MouseEvent.BUTTON1_MASK);
							robot.mouseRelease(MouseEvent.BUTTON1_MASK);
							
							overlay.clear();
                			llenar_overlay();
												
					    }
					    else 
					    {
					    	//System.out.println("Valor de zoom menos"+wheel);
					    		
					    	
					    	int intcadena = (int) (roll[currentslice][8]);
					        cadena = String.valueOf(intcadena);	
					    	    if (intcadena>1)
					    		{
					    			//System.out.println("Int cadena " + intcadena);
					    			 roll[currentslice][8] -=1;
					    		}
					    	robot.delay(1);
							robot.mousePress(MouseEvent.BUTTON1_MASK);
							robot.mouseRelease(MouseEvent.BUTTON1_MASK);
							
							overlay.clear();
                			llenar_overlay();
							
					    }
					  
					}
					            			
            			
            		}
                }   
        }
        
        
    
        
}
    
    
   
    /// La ventana principal del menu de seleccion de valores
    
    public int SliderWind() {
    	
    	
    	Ventana = new JFrame();
		 Ventana.setTitle("Ventana de prueba");
		 Ventana.setSize(400,450);
		 Ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		 Ventana.setBounds(400, 450, 426, 482);
		
		 Ventana.setLocationRelativeTo(null);
		 Ventana.setLayout(null);
		 
		 
		lblTiemposDeCorte = new JLabel("Tiempos de Corte");
		lblTiemposDeCorte.setBounds(71, 42, 130, 33);
		Ventana.add(lblTiemposDeCorte);
		
		tiempos_corte_j = new JTextField();
		tiempos_corte_j.setText("1-2");
		tiempos_corte_j.addKeyListener(new TiemposcorteKeyListener());
		tiempos_corte_j.setBounds(241, 47, 116, 22);
		Ventana.add(tiempos_corte_j);
		tiempos_corte_j.setColumns(10);
		
		lblNivelDeCorte = new JLabel("Nivel de Corte");
		lblNivelDeCorte.setBounds(71, 88, 130, 33);
		Ventana.add(lblNivelDeCorte);
		
		nivel_corte = new JTextField();
		nivel_corte.setText(String.valueOf(zs/2));
		nivel_corte.addKeyListener(new NivelcorteKeyListener());
		nivel_corte.setColumns(10);
		nivel_corte.setBounds(241, 93, 116, 22);
		Ventana.add(nivel_corte);
		
		panel = new JPanel();
		panel.setBorder(null);
		panel.setBounds(38, 162, 341, 106);
		Ventana.add(panel);
		panel.setLayout(null);
		
		lblFactorDeCorte = new JLabel("Factor de Corte");
		lblFactorDeCorte.setBounds(23, 34, 89, 16);
		panel.add(lblFactorDeCorte);
		
		menorrad = new JRadioButton("Menor que \"<\"");
		menorrad.addActionListener(new MenorqueActionListener());
		menorrad.setSelected(true);
		menorrad.setBounds(195, 9, 127, 25);
		panel.add(menorrad);
				
		mayorrad = new JRadioButton("Mayor que \">\"");
		mayorrad.addActionListener(new MayorqueActionListener());
		mayorrad.setBounds(195, 54, 127, 25);
		panel.add(mayorrad);
		
		ver_tiempos = new JButton("Ver");
		ver_tiempos.addActionListener(new VertiemposActionListener());
		ver_tiempos.setBounds(269, 288, 97, 25);
		Ventana.add(ver_tiempos);
		
		ver_todos = new JButton("Ver");
		ver_todos.addActionListener(new VertodosActionListener());
		ver_todos.setBounds(269, 329, 97, 25);
		Ventana.add(ver_todos);
		
		lblMostrarParaTodos = new JLabel("Mostrar tiempos seleccionados");
		lblMostrarParaTodos.setBounds(68, 289, 189, 22);
		Ventana.add(lblMostrarParaTodos);
		
		lblMostrarTodosLos = new JLabel("Mostrar todos los tiempos");
		lblMostrarTodosLos.setBounds(67, 330, 175, 22);
		Ventana.add(lblMostrarTodosLos);
		
		cancel = new JButton("Cancelar");
		cancel.addActionListener(new CancelButton());
		cancel.setBounds(170, 397, 97, 25);
		Ventana.add(cancel);
		 Ventana.setVisible(true);
			
		 int canwin = 0;
		 
		
		 
		 //System.out.println("Accion del boton del ver "+ ver_tiempos.getAction());
		 
		 return canwin ;
		 
    }

    // Empiezan los listener, por boton, por valor, por accion y por proceso.
    
    private class VertodosActionListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
			
			
			
			
			if(!vertiempos_pressed)
			{	
			
				time_corte = tiempos_corte_j.getText();
				lvl_corte = Integer.valueOf(nivel_corte.getText());
				
				tiempos_corte = time_corte.split("-");
				
			    
				
				if (lvl_corte > 0 && lvl_corte< zs)
				{
					flag_breaker=false;
					validar_variables = false;
					
					//System.out.println("Tiempos corte del formulario : "+time_corte+ " corte " +lvl_corte );
					//System.out.println("1 tiempo "+ Integer.parseInt(tiempos_corte[0]));
					
				}
				else
				{
					
					IJ.showMessage("El nivel de corte ingresado es incorrecto \nDebe ser un valor entre "+ 0 + " y "+ zs);
					
				}
				
				int sizethis= tiempos_corte.length;
				//System.out.println("Tamano del tiempos corte "+ sizethis);
				
				parser = new int[sizethis];
				
				for(int x =0; x < sizethis; x++)
				{
					if(!tiempos_corte[x].equals(" ") && !tiempos_corte[x].equals(""))
					{
						//System.out.println("Valor es "+ Integer.parseInt(tiempos_corte[x]));
						parser[parser_count] = Integer.parseInt(tiempos_corte[x]);
	
						parser_count++;	
					}
				}
				
				//System.out.println("En realidad los numeros son "+ parser_count  );
				enable_scroll_change = true;
				
				
				//IJ. log("The user clicked on the 'Yes' button"); 
		    	 roll =  new double[parser_count][9];
					
					for (int tps = 0; tps < parser_count; tps++ )
					{
						for (int points = 0; points < 9; points++ )
						{
							roll[tps][points] = lvl_corte;
							//System.out.println("Valor de Roll de X: "+tps +" // Y: "+ points+" es "+ roll[tps][points]);
							
						}
						
					}
					
				 anterior = new int [parser_count][9];
				 siguiente = new int [parser_count][9];
				
				 
				 
				 for (int x = 0; x<parser_count; x++)
				 {
					 for (int y = 0; y<9;y++)
					 {
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
			    
			     //calcular_borrado_inicial(parser,parser_count,choice);
				 //calcular_borrado_scroll(parser,parser_count,choice);
			     calcular_borrado_totalsintiempo(parser,parser_count,choice);	
			        
			    
		    	  
		    	  screenmod = true;
				
		    	  
		    	  
		        Ventana.setVisible(false);
	  	
		  		return;
			}
			
			else 
			{
				if(vertiempos_pressed)
				{
					//System.out.println("Aca hay que pasar los label.");
					calcular_borrado_total(parser,parser_count,choice);
					
					Ventana.setVisible(false);
					Stack_Tp.close();
				  	
			  		return;
					
				}
				
			}
		}
    	
	}
    private class TiemposcorteKeyListener  implements KeyListener
	{

			
			public void keyPressed(KeyEvent evt) {
				// TODO Auto-generated method stub
				
				int key=evt.getKeyCode();
		        String keytex = KeyEvent.getKeyText(key);
		        
		        //System.out.println("Numero "+ key +" texto " + keytex);

				
				if(key>=evt.VK_0 && key<=evt.VK_9 || key==evt.VK_ACCEPT || key==evt.VK_TAB || key==evt.VK_BACK_SPACE || key==evt.VK_NUMPAD0 || key==evt.VK_NUMPAD9 || keytex.equals("Minus"))
				{
					tiempos_corte_j.setEditable(true);
					
				}
				else 
					{
					tiempos_corte_j.setEditable(false); 
					tiempos_corte_j.setEnabled(true);
					//nivel_corte.setBackground(Color.red);
					}
				
				
			}
			
		
		@Override
		public void keyReleased(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyTyped(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}
    	
	}
    private class NivelcorteKeyListener implements KeyListener
	{

			
			public void keyPressed(KeyEvent evt) {
				// TODO Auto-generated method stub
				
				int key=evt.getKeyCode();
				if(key>=evt.VK_0 && key<=evt.VK_9 || key==evt.VK_ACCEPT || key==evt.VK_TAB || key==evt.VK_BACK_SPACE || key==evt.VK_NUMPAD0 || key==evt.VK_NUMPAD9)
				{
					nivel_corte.setEditable(true);
					
				}
				else 
					{
					nivel_corte.setEditable(false); 
					nivel_corte.setEnabled(true);
					//nivel_corte.setBackground(Color.red);
					}
				
					
			}
			
		
		@Override
		public void keyReleased(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyTyped(KeyEvent arg0) {
			// TODO Auto-generated method stub
			
		}
    	
	}
    	
    
    private class VertiemposActionListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
			
			vertiempos_pressed = true;
			
			time_corte = tiempos_corte_j.getText();
			lvl_corte = Integer.valueOf(nivel_corte.getText());
			
			tiempos_corte = time_corte.split("-");
			
		    
			
			if (lvl_corte > 0 && lvl_corte< zs)
			{
				flag_breaker=false;
				validar_variables = false;
				
				//System.out.println("Tiempos corte del formulario : "+time_corte+ " corte " +lvl_corte );
				//System.out.println("1 tiempo "+ Integer.parseInt(tiempos_corte[0]));
				
			}
			else
			{
				
				IJ.showMessage("El nivel de corte ingresado es incorrecto \nDebe ser un valor entre "+ 0 + " y "+ zs);
				
			}
			
			int sizethis= tiempos_corte.length;
			//System.out.println("Tamano del tiempos corte "+ sizethis);
			
			parser = new int[sizethis];
			
			for(int x =0; x < sizethis; x++)
			{
				if(!tiempos_corte[x].equals(" ") && !tiempos_corte[x].equals(""))
				{
					//System.out.println("Valor es "+ Integer.parseInt(tiempos_corte[x]));
					parser[parser_count] = Integer.parseInt(tiempos_corte[x]);

					parser_count++;	
				}
			}
			
			//System.out.println("En realidad los numeros son "+ parser_count  );
			enable_scroll_change = true;
			
			
			//IJ. log("The user clicked on the 'Yes' button"); 
	    	 roll =  new double[parser_count][9];
				
				for (int tps = 0; tps < parser_count; tps++ )
				{
					for (int points = 0; points < 9; points++ )
					{
						roll[tps][points] = lvl_corte;
						//System.out.println("Valor de Roll de X: "+tps +" // Y: "+ points+" es "+ roll[tps][points]);
						
					}
					
				}
				
			 anterior = new int [parser_count][9];
			 siguiente = new int [parser_count][9];
			
			 
			 
			 for (int x = 0; x<parser_count; x++)
			 {
				 for (int y = 0; y<9;y++)
				 {
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
			 //calcular_borrado_scroll(parser,parser_count,choice);
				
		        
		     ZProjector projector = new ZProjector(imageprev); 
				//System.out.println("Paso 3");
				
				projector.setMethod(ZProjector.MAX_METHOD);
				       		
		 		//proyectar sobre todo z 
				projector.setStartSlice(1 + (1-1)*zs);
				projector.setStopSlice(1*zs);
				projector.doProjection();
				
				ImagePlus projections = projector.getProjection();
					
				//projections.setWindow(new Window(projections, projections.getCanvas())); 
				Stack_Tp.setWindow(new Window(Stack_Tp, Stack_Tp.getCanvas())); 
		    	
		     
	    	  overlay = new Overlay();
	    	  numberoverlay = new Overlay();
	    	  
	    	  font = new Font("", Font.PLAIN, 24);
	    	 ////////////////////////////////////
	    	  ///// AGREGAR LA SLICEROI
	    	  
	    	  
	    	  
	    	   
	    	  stack_roi = new Roi[parser_count][9]; 
	    		
	    	  
	    	  llenar_overlay();
	    	  //System.out.println("Llenar overlay FINAL LABEL");
	    		 
	    	    win = Stack_Tp.getWindow();
		        canvas = win.getCanvas();
		        win.removeKeyListener(IJ.getInstance());
		        canvas.removeKeyListener(IJ.getInstance());
		        win.addKeyListener(new WinKeyListener());
		        canvas.addKeyListener(new WinKeyListener());
		        ImagePlus.addImageListener(new IpImageListener());
		       // IJ.log("addKeyListener");
	    	  
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
    
	private class MenorqueActionListener implements ActionListener
	{

		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
			if(menorrad.isSelected())
			{
				mayorrad.setSelected(false);
				choice="<";
			}
			
			
		}
		
	}

	private class MayorqueActionListener implements ActionListener
	{

		public void actionPerformed(ActionEvent e) 
		{
			// TODO Auto-generated method stub
			
		if(mayorrad.isSelected())
		{
			menorrad.setSelected(false);
			choice=">";
			
		}
			
			
		}

	}	
	
	private class CancelButton implements ActionListener
	{

		public void actionPerformed(ActionEvent e) 
		{
			// TODO Auto-generated method stub
		
		//System.exit(0);	
		//IJ.run("Exit");
		Ventana.setVisible(false);
			return;
		
			
		}

	}

	private class WinKeyListener implements KeyListener
	{

		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			
			int keyCode = e.getKeyCode();
	        char keyChar = e.getKeyChar();
	        int flags = e.getModifiers();
	        String keytex = KeyEvent.getKeyText(keyCode);
			
	        if (keytex.equals("Ctrl"))
	        {
	        	
	        		        	
	        	crtlpress =  true;
	        	for (int x = 0; x < 9; x++)
	        	{
	        		anterior[currentslice][x] = siguiente[currentslice][x];
	        	}
	        }
	        
	        
	        //((Stack_Tp.getCurrentSlice()) -1 )

		}

		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub
			int keyCode = e.getKeyCode();
			String keytex = KeyEvent.getKeyText(keyCode);
			 
			    
			 if (keytex.equals("Ctrl"))
		        {
				    
		        	crtlpress =  false;
		        	
		        	for (int x = 0; x < 9; x++)
		        	{
		        		siguiente[currentslice][x] = (int) roll[currentslice][x];
			        		
		        	}
		        	
		        	
		        	//System.out.println("Anterior "+ anterior[currentslice][1] +" Siguiente "+ siguiente[currentslice][1]);
	        		
		        	if(anterior[currentslice][0] != siguiente[currentslice][0] || anterior[currentslice][1] != siguiente[currentslice][1] || anterior[currentslice][2] != siguiente[currentslice][2] 
		        			|| anterior[currentslice][3] != siguiente[currentslice][3] || anterior[currentslice][4] != siguiente[currentslice][4] || 
		        			anterior[currentslice][5] != siguiente[currentslice][5] || anterior[currentslice][6] != siguiente[currentslice][6] || 
		        			anterior[currentslice][7] != siguiente[currentslice][7] || anterior[currentslice][8] != siguiente[currentslice][8])
		        	{
		        		//System.out.println("Hay que hacer la wea");
		        		//System.out.println("Coordenas X prueba "+ Inter1[0]+" / "+ Inter1[1]+" / "+ Inter1[2] );
		        		//System.out.println("Coordenas Y prueba "+ Inter2[0]+" / "+ Inter2[1]+" / "+ Inter2[2] );
		        		
		        		//System.out.println("Valor Xlvl"+Xlvl[0] +" / "+Xlvl[1]);
		        		
		        		
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
			// TODO Auto-generated method stub
			
		}

		
		
	}
	
	
	private class IpImageListener implements ImageListener
	{

		@Override
		public void imageClosed(ImagePlus e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void imageOpened(ImagePlus e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void imageUpdated(ImagePlus e) {
			// TODO Auto-generated method stub
			
		//	System.out.println("ASDASDasdasdasd");
			
			currentslice= ((Stack_Tp.getCurrentSlice()) -1 );
	    	
			overlay.clear();
			llenar_overlay();
		}

	}
	
    


	public void imageClosed(ImagePlus e) {
		
	}


	@Override
	public void imageOpened(ImagePlus e) {
		
	}


	@Override
	public void imageUpdated(ImagePlus e) {
		
		
		
		
		
	}


	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}
	 public static void main(String[] args) {
			// NO AGREGAR MAS LINEAS EN EL MAIN
			new ImageJ();
			System.out.println("Working Directory = " + System.getProperty("user.dir"));
			//ImagePlus image = IJ.openVirtual("Z:/Eclipse/Fijithelium/examples/Stack/video_animal1.tif");
			//ImagePlus image = IJ.openVirtual("Z:/Eclipse/Fijithelium/examples/stack_t001.tif");
			//ImagePlus image = IJ.openVirtual("Z:/Eclipse/Fijithelium/examples/Stack/stack_t01-t10.tif");
			
			ImagePlus image = IJ.openVirtual("C:/Users/Alejandro/Desktop/java/epithelium-projection/examples/sshort sequence-1.tif");
			
			
			
			
			IJ.runPlugIn(image, "Proyeccion_General_Final", "parameter=value");
			// image.show();
			 WindowManager.addWindow(image.getWindow());
		}
}
