import flanagan.analysis.SurfaceSmooth;

class Test {

  static public void main(String[] args) {
    double[] xData = new double[] { 0, 10, 20 };
    double[] yData = new double[] { 0, 10, 20 };
    double[][] zData = new double[3][3];
    zData[0][0] = 1;
    zData[0][1] = 1;
    zData[0][2] = 1;
    zData[1][0] = 0;
    zData[1][1] = 0;
    zData[1][2] = 0;
    zData[2][0] = 1;
    zData[2][1] = 1;
    zData[2][2] = 1;
    SurfaceSmooth surface = new SurfaceSmooth(xData, yData, zData);
   // System.out.println(surface.interpolateSavitzkyGolay(0.5, 0.5));
    System.out.println(surface.savitzkyGolay(3, 3)[0][0]);
  }
}
