package VolumePeeler;
import ij.*;
import ij.process.*;
import ij.plugin.*; 
import ij.plugin.filter.PlugInFilter;

import ij.gui.*;
import ij.measure.ResultsTable;

import java.awt.*;
import java.awt.event.*;
import java.io.*;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;



public class Spline_Projection
implements PlugInFilter, ActionListener, KeyListener, ItemListener, ImageListener, MouseListener, DialogListener {

  private static int frames, width, height, slices, channel, channelV;
  private boolean preview = true, okPressed = false;

  private final static String PLUGIN_NAME = "VolumePeeler.Spline_Projection";
  private final static String WINDOW_TITLE = PLUGIN_NAME;

  private ImagePlus originalImage, processedImage;
  private ImagePlus projectionsImage;

  private Button previewButton, processButton, ABCButton, repeatValuesButton, copyMatrixButton, saveButton, loadButton;
  private int toRepeatValue=0;
  private int tamanooffset=6;
  
  private int[][] offset;
  private TextField[] tfQuadrant;
  private TextField anchoBanda;
  private boolean[] frameEnabled, gridEnabled;
  private Checkbox frameEnabledCheckbox, anteriorCheckbox, posteriorCheckbox, bandCheckbox, gridCheckbox;
  private boolean keepAnteriorPart = false, bandSelection =false;
  private int ancho=0; //Ancho de la banda
  
  public int setup(String arg, ImagePlus imp) {
    return STACK_REQUIRED | DOES_ALL;
  }
  
  public void run(ImageProcessor ip) {
    ImagePlus image = WindowManager.getCurrentImage(); 
    originalImage = image.duplicate();
    processedImage = image.duplicate();
    channel = image.getNChannels(); //se agrega la información de canales
    frames = image.getNFrames(); 
    slices = image.getStack().getSize()/(frames*channel); // se agrega la información de canales
    width = image.getStack().getWidth();
    height = image.getStack().getHeight();
    //
    GenericDialog gd0 = new GenericDialog("Please select number of control points ");
    gd0.addSlider("Select n for n^2 control points", 3, tamanooffset, 3);
    gd0.addDialogListener(this);
    gd0.showDialog();
    
    okPressed = gd0.wasOKed();
    preview = false;
    tamanooffset=(int)gd0.getNextNumber();
    System.out.println(tamanooffset);

    	
    if (channel == 1){
      System.out.println("Un Canal");
      this.channelV=1;
    }
    else {
      System.out.println("Mas de un Canal");
      GenericDialog gd = new GenericDialog("More than 1 channel detected");
      gd.addSlider("Please select 1 channel", 1, channel, 1);
      gd.addDialogListener(this);
      gd.showDialog();
      
      okPressed = gd.wasOKed();
      preview = false;
      System.out.println("Canal seleccionado");
      System.out.println(channelV);



    }

    gridEnabled = new boolean[1];
    frameEnabled = new boolean[frames];
    frameEnabled[0] = frameEnabled[frames - 1] = true;
    initOffsetsMatrix();
    showProjections();
  }

  private void initOffsetsMatrix() {
    offset = new int[frames][tamanooffset*tamanooffset];
    for (int frame = 0; frame < frames; frame++) {
      for (int i = 0; i < tamanooffset*tamanooffset; i++) {
        offset[frame][i] = slices;
      }
    }
  }

  private void showProjections() {
    ImageStack projectionsStack = new ImageStack(width, height);
    ImagePlus[] channels = ChannelSplitter.split(originalImage);

    ZProjector projector = new ZProjector(channels[channelV-1]); 
    projector.setMethod(ZProjector.MAX_METHOD);
    // System.out.println(channels[0].getNChannels());
    // System.out.println(channels[0].getNFrames());
    // System.out.println(channels[0].getStack().getSize());
    // System.out.println("0---------0");
    for(int t = 0; t < frames; t++) {
      projector.setStartSlice(t * slices);
      // System.out.println("inicio");
      // System.out.println(t * slices);

      projector.setStopSlice((t + 1) * slices - 1);
      // System.out.println("fin");
      // System.out.println((t + 1) * slices - 1);
 
      projector.doProjection();
      projectionsStack.addSlice(projector.getProjection().getProcessor());          
            
           
    }
    projectionsImage = new ImagePlus(WINDOW_TITLE, projectionsStack);
    projectionsImage.show();
    ImagePlus.addImageListener(this);
    buildUI();
  }

  private void updateOffsets(int slice) {
	  
    this.frameEnabledCheckbox.setState(frameEnabled[slice - 1]);
    this.previewButton.setEnabled(frameEnabled[slice - 1]);
    this.repeatValuesButton.setEnabled(false);
    
    //saca el cursos
    for (int i = 0; i < tamanooffset*tamanooffset; i++) {
      tfQuadrant[i].transferFocus();
    }
    
    //desactiva/activa los textField y cambia los valores segun el frame
    for (int i = 0; i < tamanooffset*tamanooffset; i++) {
        tfQuadrant[i].setEnabled(frameEnabled[slice - 1]);
        tfQuadrant[i].setText("" + offset[slice - 1][i]);
      }
    //actualiza la matriz de textfield actual
    if (true){

      int currentFrame = projectionsImage.getCurrentSlice() - 1;
      for (int i = 0; i < tamanooffset*tamanooffset; i++) {
            tfQuadrant[i].setText("" + offset[currentFrame][i] );
      }
    }
  }

  //private void buildOverlay() {
 //   Overlay overlay = new Overlay();
 //   drawLineInOverlay(overlay, width / 3, 0, width / 3, height);
 //   drawLineInOverlay(overlay, 2 * width / 3, 0, 2 * width / 3, height);
 //   drawLineInOverlay(overlay, 0, height / 3, width, height / 3);
 //   drawLineInOverlay(overlay, 0, 2 * height / 3, width, 2 * height / 3);
 //   projectionsImage.setOverlay(overlay);
 // }
  private void buildOverlay() {
	    Overlay overlay = new Overlay();
	    for (int i = 0; i < tamanooffset-1; i++) {
		    drawLineInOverlay(overlay, (i+1) * width / tamanooffset, 0, (i+1) * width / tamanooffset, height);
		    drawLineInOverlay(overlay, 0, (i+1) * height / tamanooffset, width, (i+1) * height / tamanooffset);
	    	
	    }
	    projectionsImage.setOverlay(overlay);
	  }

  private void drawLineInOverlay(Overlay overlay, int x1, int y1, int x2, int y2) {
    Roi line = new Line(x1, y1, x2, y2);
    line.setStrokeColor(Color.green);
    line.setStrokeWidth(1);
    overlay.add(line);
  }
  
  

  private void buildUI() {
    tfQuadrant = new TextField[tamanooffset*tamanooffset];
    Panel container = new Panel();
    container.setLayout(new FlowLayout());
    Panel theNumbers = new Panel();
    theNumbers.setLayout(new GridLayout(tamanooffset, tamanooffset));
    for (int i = 0; i < tamanooffset*tamanooffset; i++) {
      tfQuadrant[i] = new TextField("" + offset[0][i]);
      tfQuadrant[i].setName("" + i);
      tfQuadrant[i].addKeyListener(this);
      tfQuadrant[i].addMouseListener(this);
      theNumbers.add(tfQuadrant[i]);
    }
    this.frameEnabledCheckbox = new Checkbox("Use this frame for interpolation                        ", frameEnabled[0]);
    this.frameEnabledCheckbox.addItemListener(this);
    
    this.gridCheckbox = new Checkbox("Grid", gridEnabled[0]);
    this.gridCheckbox.addItemListener(this);
    

    CheckboxGroup choice = new CheckboxGroup();
    this.anteriorCheckbox = new Checkbox("Keep higher z", choice, keepAnteriorPart);
    this.anteriorCheckbox.addItemListener(this);
    this.posteriorCheckbox = new Checkbox("Keep lower z", choice, !keepAnteriorPart);
    this.posteriorCheckbox.addItemListener(this);
    this.bandCheckbox = new Checkbox("Band", choice, bandSelection);
    this.bandCheckbox.addItemListener(this);
    this.anchoBanda = new TextField();
    bandSelection= bandCheckbox.getState();
    anchoBanda.setEnabled(bandSelection);
    anchoBanda.setText("" + ancho);
    
        
    Panel choiceContainer = new Panel(new GridLayout(4, 1));
    choiceContainer.add(anteriorCheckbox);
    choiceContainer.add(posteriorCheckbox);
    choiceContainer.add(bandCheckbox);
    choiceContainer.add(anchoBanda);
    
        
    previewButton = new Button("Preview this frame");
    previewButton.addActionListener(this);
    processButton = new Button("Process all frames");
    processButton.addActionListener(this);

    ABCButton = new Button("Brightness/Contrast");
    ABCButton.addActionListener(this);


    Panel buttonsContainer = new Panel(new GridLayout(3, 1));
    buttonsContainer.add(previewButton);
    buttonsContainer.add(processButton);
    buttonsContainer.add(ABCButton);

    repeatValuesButton = new Button("Repeat z in this frame");
    repeatValuesButton.setEnabled(false);
    repeatValuesButton.addActionListener(this);
    copyMatrixButton   = new Button("Repeat matrix in all frames");
    copyMatrixButton.addActionListener(this);
    
    saveButton   = new Button("Save all matrices");
    saveButton.addActionListener(this);
    loadButton   = new Button("Load all matrices");
    loadButton.addActionListener(this);
    
    Panel checkboxContainer = new Panel(new GridLayout(1, 2));
    checkboxContainer.add(frameEnabledCheckbox);
    checkboxContainer.add(gridCheckbox);

    
    Panel buttonsShortcuts = new Panel(new GridLayout(4, 1));
    buttonsShortcuts.add(repeatValuesButton);
    buttonsShortcuts.add(copyMatrixButton);
    buttonsShortcuts.add(saveButton);
    buttonsShortcuts.add(loadButton);
    
    
    Panel labelsContainer = new Panel(new GridLayout(4, 1));
    labelsContainer.add(new Label("Modify Z  "));
    labelsContainer.add(new Label("matrix to "));
    labelsContainer.add(new Label("improve   "));
    labelsContainer.add(new Label("visualization."));
    
    container.add(labelsContainer);
    container.add(theNumbers);
    container.add(buttonsShortcuts);
    container.add(choiceContainer);
    container.add(buttonsContainer);
    
    projectionsImage.getWindow().add(checkboxContainer);
    projectionsImage.getWindow().add(container);
    
    String html="v1.9, by SCIAN-Lab 2023, Mauricio.Cerda@uchile.cl";
   
    projectionsImage.getWindow().add( new Label(html) );
    projectionsImage.getWindow().pack();
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {

	  
	if (e.getSource() == previewButton) {

	      int sliceIndex = projectionsImage.getCurrentSlice();
	      projectionsImage.getStack().setProcessor(preview(sliceIndex,this.channelV).getProcessor(), sliceIndex);
	      projectionsImage.updateAndDraw();
	
	      
	    }
    else if (e.getSource() == ABCButton) {
      IJ.run("Brightness/Contrast...");
    

    }
    else if (e.getSource() == processButton) {
      processAllFrames();
    }
    else if (e.getSource() == repeatValuesButton) {
    	//toRepeatValue
    	int currentFrame = projectionsImage.getCurrentSlice() - 1;
        for (int i = 0; i < tamanooffset*tamanooffset; i++) {
            offset[currentFrame][i] = toRepeatValue;
            tfQuadrant[i].setText("" + toRepeatValue );
        }
        repeatValuesButton.setEnabled(false);

    }
    else if ( e.getSource() == copyMatrixButton) {
    	
    	int currentFrame = projectionsImage.getCurrentSlice() - 1;
    	
        for (int frame = 0; frame < frames; frame++) {
            for (int i = 0; i < tamanooffset*tamanooffset; i++) {
              offset[frame][i] = offset[currentFrame][i];
            }
          }
    }
    else if ( e.getSource() == saveButton) {
        Frame frame = new Frame("File Chooser Example");

        FileDialog fileDialog = new FileDialog(frame, "Select File");
        fileDialog.setMode(FileDialog.SAVE);
        fileDialog.setVisible(true);

        // Get the selected file
        String filename = fileDialog.getDirectory() + fileDialog.getFile();


    	try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (int[] row : offset) {
                for (int num : row) {
                    writer.print(num + " ");

                }
                writer.println();

            }
            System.out.println("Array written to file successfully!");

            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    	
    
    else if ( e.getSource() == loadButton) {
    	Frame frame = new Frame("File Chooser Example");

        FileDialog fileDialog = new FileDialog(frame, "Select File");
        fileDialog.setMode(FileDialog.LOAD);
        fileDialog.setVisible(true);

        // Get the selected file
        String filename = fileDialog.getDirectory() + fileDialog.getFile();
        int[][] twoDimensional = offset;

        try {
            FileReader fileReader = new FileReader(filename);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            
            String line;
            int framess= 0;
           
            while ((line = bufferedReader.readLine()) != null) {
                String[] numbers = line.split(" ");
                if (numbers.length != tamanooffset * tamanooffset) {
                	GenericDialog gdp = new GenericDialog("The saved matrix does not match the selected size.");
                	gdp.addMessage("The saved matrix does not match the selected size.");
                	gdp.showDialog();

                	break;
                }
                for (int k = 0; k < numbers.length; k++) {
                    twoDimensional[framess][k] = Integer.parseInt(numbers[k]);
                }
                framess=framess+1;
                System.out.println(numbers.length);

            }
            
            bufferedReader.close();
            offset=twoDimensional;
            updateOffsets(1);
            
        } catch (IOException ee) {
            ee.printStackTrace();
        }     
        
        
    
    	
    }
	  
  }
  
  private ImagePlus preview(int frame, int selectedchannel) {
    int f = frame - 1;
    
    /*
    double[] x1Data = { width / 6, width / 2, 5 * width / 6 };
    double[] x2Data = { height / 6, height / 2, 5 * height / 6 };
    double[][] yData = {
      { offset[f][0], offset[f][3], offset[f][6] },
      { offset[f][1], offset[f][4], offset[f][7] },
      { offset[f][2], offset[f][5], offset[f][8] }
    };
    */
    double[] x1Data = new double[tamanooffset];
    double[] x2Data = new double[tamanooffset];

    for (int i = 0; i < tamanooffset; i++) {
    	x1Data[i] = (2 *width * i + 1) / (double) (2 * tamanooffset);
    	x2Data[i] = (2 *height * i + 1) / (double) (2 * tamanooffset);

    }
    double[][] yData = new double[tamanooffset][tamanooffset];
    for (int i = 0; i < tamanooffset; i++) {
        for (int j = 0; j < tamanooffset; j++) {
            yData[i][j] = offset[f][(j * tamanooffset) + i];
        }
    }
    
    BiCubicSpline surface = new BiCubicSpline(x1Data, x2Data, yData);
    double[][] interps = new double[width][height];
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        interps[i][j] = surface.interpolate(i, j);
      }
    }
    //double[][] pixels = new double[width][height];
    ImagePlus[] channels = ChannelSplitter.split(processedImage); //agregada CS
    ImageStack stack = channels[selectedchannel-1].getStack();
    ImageStack originalImageonechannel=ChannelSplitter.split(originalImage)[selectedchannel-1].getStack();
    if (bandSelection) { 
      //caso banda
      ancho=Integer.parseInt(this.anchoBanda.getText());
      for (int k = 0; k < slices; k++) {
        int z = f * slices + k;
        // f*(slices*nchanells)+ slices*(chanell-1)+k
        
        for (int i = 0; i < width; i++) {
          for (int j = 0; j < height; j++) {
            if (( Math.round(interps[i][j]+ancho) >= k+1) &&  (Math.round(interps[i][j]-ancho) <= k+1)) {
              stack.setVoxel(i, j, z, originalImageonechannel.getVoxel(i, j, z));
            }
            else {
              stack.setVoxel(i, j, z, 0);
            }
          }
        }
      }
    }
    else {
    //caso keep lower-higher
      for (int k = 0; k < slices; k++) {
        int z = f * slices + k;
        
        for (int i = 0; i < width; i++) {
          for (int j = 0; j < height; j++) {
            if ((keepAnteriorPart && Math.round(interps[i][j]) >= k+1) ||
              (!keepAnteriorPart && Math.round(interps[i][j]) <= k+1)) {
              stack.setVoxel(i, j, z, 0);
            }
            else {
              stack.setVoxel(i, j, z, originalImageonechannel.getVoxel(i, j, z));
            }
          }
        }
      }
    }
    channels[selectedchannel-1].setStack(stack);
    
    //ZProjector projector = new ZProjector(processedImage); 
    ZProjector projector = new ZProjector(channels[selectedchannel-1]); 

    projector.setMethod(ZProjector.MAX_METHOD);
    projector.setStartSlice(f * slices + 1);
    projector.setStopSlice((f + 1) * slices);
    projector.doProjection();
    
    ImagePlus result = projector.getProjection();
    
    //normalizacion?
    //ContrastEnhancer ce = new ContrastEnhancer();
    //ce.setNormalize(true);
    //ce.stretchHistogram(result, 0.1);
    
    return result;
  }
  
  private void processAllFrames() {

	  ResultsTable table = new ResultsTable();

	  for (int cuadrante = 0; cuadrante < tamanooffset*tamanooffset; cuadrante++) {
		  int frameInicioInterpolacion = 0;
		  for (int frame = 1; frame < frames; frame++) {
			  if (frameEnabled[frame]) {
				  double m = (offset[frame][cuadrante] - offset[frameInicioInterpolacion][cuadrante]) / (1.0 * (frame - frameInicioInterpolacion));
				  for (int i = frameInicioInterpolacion + 1; i < frame; i++) {
					  offset[i][cuadrante] = (int)(offset[frameInicioInterpolacion][cuadrante] + m * (i - frameInicioInterpolacion));
				  }
				  frameInicioInterpolacion = frame;
			  }
		  }
	  }
	  ImageStack projectionsStack = new ImageStack(width, height);
	  for (int i=1; i <=channel; i++){

	    for (int frame = 1; frame <= frames; frame++) {
		    //projectionsStack.addSlice(Integer.toString(index),preview(frame,i).getProcessor());
        projectionsStack.addSlice(preview(frame,i).getProcessor());

		    if (i==1)
          addRowToTable(table, frame);
          
          
	  }
    }
    //ImageStack projectionsStack = new ImageStack(width, height);
	  ImagePlus result = new ImagePlus("Result", projectionsStack);
    
    new HyperStackConverter();
    //result.setDimensions(result.getNChannels()*channel, result.getNSlices()/channel, result.getNFrames());
    //ImagePlus result2= result;
    result= HyperStackConverter.toHyperStack(result, result.getNChannels()*channel, result.getNSlices()/channel, result.getNFrames(),"xyzct","color");
    result.show();
	  table.show("Spline Projection data");
  }

 /* private void addRowToTable(ResultsTable table, int frame) {
	  table.incrementCounter();
	  table.addValue("Frame", frame);
	  
	  table.addValue("Z11", offset[frame-1][0]);
	  table.addValue("Z12", offset[frame-1][1]);
	  table.addValue("Z13", offset[frame-1][2]);
	  table.addValue("Z21", offset[frame-1][3]);
	  table.addValue("Z22", offset[frame-1][4]);
	  table.addValue("Z23", offset[frame-1][5]);
	  table.addValue("Z31", offset[frame-1][6]);
	  table.addValue("Z32", offset[frame-1][7]);
	  table.addValue("Z33", offset[frame-1][8]);
	  table.addValue("Higher", ""+this.keepAnteriorPart);
    table.addValue("Interpolation", ""+frameEnabled[frame-1] );//Nueva columna
    table.addValue("Band", ""+ ancho*Boolean.compare(bandSelection, false) );//Nueva columna


  }*/
  private void addRowToTable(ResultsTable table, int frame) {
	  table.incrementCounter();
	  table.addValue("Frame", frame);
      for (int i = 0; i < tamanooffset; i++) {
    	  for (int j = 0; j < tamanooffset; j++) {
    		  table.addValue("Z"+(i+1)+(j+1), offset[frame-1][i*tamanooffset+j]);
    	  }
    	      
      }
	  
	  table.addValue("Higher", ""+this.keepAnteriorPart);
	  table.addValue("Interpolation", ""+frameEnabled[frame-1] );//Nueva columna
	  table.addValue("Band", ""+ ancho*Boolean.compare(bandSelection, false) );//Nueva columna


  }
  
  @Override
  public void imageClosed(ImagePlus imp) {
	  ImagePlus.removeImageListener(this);
  }

  @Override
  public void imageOpened(ImagePlus imp) {}

  @Override
  public void imageUpdated(ImagePlus imp) {
	if ( WindowManager.getCurrentImage() == null) {
		ImagePlus.removeImageListener(this);
	}else if (WindowManager.getCurrentImage().getTitle().indexOf(WINDOW_TITLE) == 0) {
		if( imp.getCurrentSlice() < frames) {
			updateOffsets(imp.getCurrentSlice());
		}else {
			updateOffsets(1);
		}
			
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {}

  @Override
  public void keyReleased(KeyEvent e) {
    int quadrantIndex = Integer.parseInt(((TextField)(e.getSource())).getName());
    int sliceIndex = projectionsImage.getCurrentSlice();
    try {
      offset[sliceIndex - 1][quadrantIndex] = Integer.parseInt(((TextField)e.getSource()).getText());
    }
    catch (Exception ex) {
    }
    
    try {
	toRepeatValue=Integer.parseInt( ((TextField)(e.getSource())).getText() );
    }
    catch (Exception ex) {
    	toRepeatValue=slices;
    }

    
	if (toRepeatValue > slices) {
		toRepeatValue = slices;
	}
	if ( toRepeatValue < 1) {
		toRepeatValue = 1;
	}
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  @Override
  public void itemStateChanged(ItemEvent e) {
	  
	  if (e.getSource() == gridCheckbox) {
		  if (gridEnabled[0]== false) {
			  buildOverlay();
		  	  gridEnabled[0]= true;}
		  else if (gridEnabled[0]== true) {
			  Overlay overlay = new Overlay();
			  projectionsImage.setOverlay(overlay);
			  gridEnabled[0]= false;}
	  
	      
	    }
    else if (e.getSource() == anteriorCheckbox) {
      keepAnteriorPart = true;
      bandSelection= bandCheckbox.getState();
      this.anchoBanda.setEnabled(bandSelection);


    }
    else if (e.getSource() == posteriorCheckbox) {
      keepAnteriorPart = false;
      bandSelection= bandCheckbox.getState();
      this.anchoBanda.setEnabled(bandSelection);

    }
    else if (e.getSource() == bandCheckbox) {
      keepAnteriorPart = false;
    }
    if (bandCheckbox.getState()){
      bandSelection= bandCheckbox.getState();
      this.anchoBanda.setEnabled(bandSelection);
    }

    else if (e.getSource() == frameEnabledCheckbox) {
      boolean value = frameEnabledCheckbox.getState();
      int currentFrame = projectionsImage.getCurrentSlice() - 1;
      if (currentFrame == 0 || currentFrame == frames - 1) {
        frameEnabledCheckbox.setState(true);
        return;
      }
      frameEnabled[currentFrame] = value;
      for (int i = 0; i < tamanooffset*tamanooffset; i++) {
        tfQuadrant[i].setEnabled(value);
      }
      previewButton.setEnabled(value);
    }
  }
  
  public static void main(String[] args) {

    new ImageJ();
    ImagePlus image = IJ.openImage(args[0]);      
    IJ.runPlugIn(image, PLUGIN_NAME, "parameter=value");
    WindowManager.addWindow(image.getWindow());
  }

@Override
public void mouseClicked(MouseEvent e) {

    try {
    	((TextField)(e.getSource())).selectAll();
    	toRepeatValue=Integer.parseInt( ((TextField)(e.getSource())).getText() );
    	
    }
    catch (Exception ex) {
    	toRepeatValue=slices;
    }
    
	repeatValuesButton.setEnabled(true);

}

@Override
public void mousePressed(MouseEvent e) {
	// TODO Auto-generated method stub
	
}

@Override
public void mouseReleased(MouseEvent e) {
	
    try {
	toRepeatValue=Integer.parseInt( ((TextField)(e.getSource())).getText() );
    }
    catch (Exception ex) {
    	toRepeatValue=slices;
    }
    
	repeatValuesButton.setEnabled(true);
	
}

@Override
public void mouseEntered(MouseEvent e) {
	// TODO Auto-generated method stub
	
}

@Override
public void mouseExited(MouseEvent e) {
	// TODO Auto-generated method stub
	
}

@Override
public boolean dialogItemChanged(GenericDialog arg0, AWTEvent arg1) {
  // TODO Auto-generated method stub
  arg0.getSliders();
  channelV = (int) ((Scrollbar)(arg0.getSliders().get(0))).getValue();
  //System.out.println(channelV);
  
  return true;
}  
}
