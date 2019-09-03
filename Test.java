import flanagan.interpolation.*;

class Test {

  static public void main(String[] args) {
    double[] x1Data = { 0, 10, 20 };
    double[] x2Data = { 0, 10, 20 };
    double[][] yData = {
      { 0, 0, 0 },
      { 1, 1, 1 },
      { 2, 2, 2 }
    };
    BiCubicSpline surface = new BiCubicSpline(x1Data, x2Data, yData);
    // System.out.println(surface.interpolateSavitzkyGolay(0.5, 0.5));
    System.out.println(surface.interpolate(15, 15));
  }
}
