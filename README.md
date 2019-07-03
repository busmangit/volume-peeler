La imagen y el classpath se configuran en .vscode/launch.json

Para compilar:

`
javac -cp "C:\Fiji.app\jars\ij-1.52n.jar" Proyeccion_Esfera.java
`

Para ejecutar:
`
java -Xmx16g -cp "C:\Fiji.app\jars\ij-1.52n.jar;." Proyeccion_Esfera ./examples/ejemplo_esfera.tif
`