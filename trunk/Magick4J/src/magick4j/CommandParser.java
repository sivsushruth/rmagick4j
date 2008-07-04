package magick4j;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO Apply transformation to the primitives.
import java.util.Stack;

/**
 * See http://studio.imagemagick.org/script/magick-vector-graphics.php for specs.
 */
public class CommandParser {

    private interface ParserBuilder {

        Command build(String... parts);
    }
    private static final Map<String, ParserBuilder> BUILDERS = buildBuilders();

    private static Map<String, ParserBuilder> buildBuilders() {
        Map<String, ParserBuilder> builders = new HashMap<String, ParserBuilder>();
        
        builders.put("affine", new ParserBuilder(){
            public Command build( String... parts){
                String[] args = parts[1].split(",");
                double sx = Double.parseDouble(args[0]);
                double rx = Double.parseDouble(args[1]);
                double ry = Double.parseDouble(args[2]);
                double sy = Double.parseDouble(args[3]);
                double tx = Double.parseDouble(args[4]);
                double ty = Double.parseDouble(args[5]);
                return CommandBuilder.affine(sx, rx, ry, sy, tx, ty);
            }
        });
        
        builders.put("arc", new ParserBuilder(){
            public Command build(String... parts){
                String[] originPoint = parts[1].split(",");
                String[] endPoint = parts[2].split(",");
                String[] degrees = parts[3].split(",");
                
                Point2D origin = new Point2D.Double(Double.parseDouble(originPoint[0]),Double.parseDouble(originPoint[1]));
                Point2D end = new Point2D.Double(Double.parseDouble(endPoint[0]),Double.parseDouble(endPoint[1]));
                double arcStart = Double.parseDouble(degrees[0]);
                double arcStop = Double.parseDouble(degrees[1]);
                
                return CommandBuilder.shape(new Arc2D.Double(origin.getX(), origin.getY(), end.getX()-origin.getX(), end.getY()-origin.getY(), -arcStart, -(arcStop - arcStart), Arc2D.OPEN));
            }
        });
        
        builders.put("bezier", new ParserBuilder(){
           public Command build(String... parts){
               List<Command> commands = new ArrayList<Command>();
               // TODO Study how bezier command work depending on the ammount
               // of points.
               return CommandBuilder.compose(commands);
           } 
        });
        
        builders.put("circle", new ParserBuilder() {
            public Command build(String... parts) {
                String[] args0 = parts[1].split(",");
                String[] args1 = parts[2].split(",");
                double centerX = Double.parseDouble(args0[0]);
                double centerY = Double.parseDouble(args0[1]);
                double perimeterX = Double.parseDouble(args1[0]);
                double perimeterY = Double.parseDouble(args1[1]);
                double dX = perimeterX - centerX;
                double dY = perimeterY - centerY;
                double radius = Math.sqrt(dX * dX + dY * dY);
                return CommandBuilder.shape(new Ellipse2D.Double(centerX - radius, centerY - radius, 2 * radius, 2 * radius));
            }
        });
        
        builders.put("ellipse", new ParserBuilder() {
            public Command build(String... parts) {
                String[] args0 = parts[1].split(",");
                String[] args1 = parts[2].split(",");
                String[] args2 = parts[3].split(",");
                double centerX = Double.parseDouble(args0[0]);
                double centerY = Double.parseDouble(args0[1]);
                double radiusX = Double.parseDouble(args1[0]);
                double radiusY = Double.parseDouble(args1[1]);
                double arcStart = Double.parseDouble(args2[0]);
                double arcStop = Double.parseDouble(args2[1]);
                // TODO Custom primitive class to support OPEN strokes and PIE fills?
                return CommandBuilder.shape(new Arc2D.Double(centerX - radiusX, centerY - radiusY, 2 * radiusX, 2 * radiusY, -arcStart, -(arcStop - arcStart), Arc2D.OPEN));
            }
        });
        
        builders.put("fill", new ParserBuilder() {
            public Command build(String... parts) {
                String colorName = parts[1].replace("\"", "");
                return CommandBuilder.fill(colorName);
            }
        });
        
        builders.put("fill-opacity", new ParserBuilder() {
            public Command build(String... parts) {
                double opacity = Double.parseDouble(parts[1]);
                return CommandBuilder.fillOpacity(opacity);
            }
        });
        
        builders.put("line", new ParserBuilder() {
            public Command build(String... parts) {
                String[] args0 = parts[1].split(",");
                String[] args1 = parts[2].split(",");
                double x1 = Double.parseDouble(args0[0]);
                double y1 = Double.parseDouble(args0[1]);
                double x2 = Double.parseDouble(args1[0]);
                double y2 = Double.parseDouble(args1[1]);
                // TODO Custom primitive to avoid fills?
                return CommandBuilder.shape(new Line2D.Double(x1, y1, x2, y2));
            }
        });
        
        builders.put("path", new ParserBuilder() {
            public Command build(String... parts){
                String commandLine = "";
                
                // Join the parts.
                for(int i=1; i<parts.length; i++){
                    commandLine += parts[i] + " ";
                }
                
                commandLine = commandLine.replaceAll("'", "");
                commandLine = commandLine.replaceAll(",", ""); // RMagick4J must be capable of recognize both comma separated values and blank space separated values.
                commandLine = commandLine.trim();
                commandLine += "Z";
                
                // Prepend a Z to every M or m. This way, parsing will be easier.
                for(int i=1; i<commandLine.length(); i++){
                    
                    char c = commandLine.charAt(i);
                    if(c == 'M' || c == 'm'){
                        commandLine = commandLine.substring(0,i) + "Z" + commandLine.substring(i);
                        i++; // Don't touch this.
                    }
                }
                
                String currentCommand = "";
                Point2D currentPoint = new Point2D.Double(0,0);
                GeneralPath path = new GeneralPath();
                
                while(!commandLine.equals("")){
                    
                    // Get the current command.
                    int i = 0;
                    for(i=1; i<commandLine.length(); i++){
                        if(Character.isLetter(commandLine.charAt(i))){
                            break;
                        }
                    }
                    
                    currentCommand = commandLine.substring(0, i);
                    commandLine = commandLine.substring(i);
                    
                    if(currentCommand.length() > 1 && currentCommand.charAt(1) != ' '){
                        currentCommand =    currentCommand.substring(0,1)+
                                            " "+
                                            currentCommand.substring(1);
                    }
                    
                    String[] params = currentCommand.split(" ");
                    
                    
                    //TODO: Finish
                    switch(params[0].charAt(0)){
                        case 'H':
                            for(i = 1; params.length - i > 0; i++){
                                currentPoint = new Point2D.Double(Double.parseDouble(params[i]), currentPoint.getY());
                                path.lineTo((float) currentPoint.getX(), (float) currentPoint.getY());
                            }
                            break;
                            
                        case 'h':
                            for(i = 0; params.length - i > 0; i++){
                                currentPoint = new Point2D.Double(  currentPoint.getX() + Double.parseDouble(params[i]),
                                                                    currentPoint.getY());
                                path.lineTo((float) currentPoint.getX(), (float) currentPoint.getY());
                            }
                            break;
                            
                        case 'L':
                            for(i = 1; params.length - i >= 2; i+=2){
                                currentPoint = new Point2D.Double(Double.parseDouble(params[i]), Double.parseDouble(params[i+1]));
                                path.lineTo((float) currentPoint.getX(), (float) currentPoint.getY());
                            }
                            break;
                            
                        case 'l':
                            for(i = 1; params.length - i >= 2; i+=2){
                                currentPoint = new Point2D.Double(  currentPoint.getX()+Double.parseDouble(params[i]),
                                                                    currentPoint.getY()+Double.parseDouble(params[i+1]));
                                path.lineTo((float) currentPoint.getX(), (float) currentPoint.getY());
                            }
                            break;
                        
                        
                        case 'M':
                            for(i = 1; params.length - i >= 2; i+=2){
                                currentPoint = new Point2D.Double(Double.parseDouble(params[i]), Double.parseDouble(params[i+1]));
                                if(i==0){
                                    path.moveTo((float) currentPoint.getX(), (float) currentPoint.getY());
                                } else {
                                    path.lineTo((float) currentPoint.getX(), (float) currentPoint.getY());
                                }
                            }
                            break;
                            
                        case 'm':
                            for(i = 1; params.length - i >= 2; i+=2){
                                currentPoint = new Point2D.Double(  currentPoint.getX()+Double.parseDouble(params[i]),
                                                                    currentPoint.getY()+Double.parseDouble(params[i+1]));
                                if(i==0){
                                    path.moveTo((float) currentPoint.getX(), (float) currentPoint.getY());
                                } else {
                                    path.lineTo((float) currentPoint.getX(), (float) currentPoint.getY());
                                }
                            }
                            break;
                            
                        case 'V':
                            for(i = 1; params.length - i > 0; i++){
                                currentPoint = new Point2D.Double(currentPoint.getX(), Double.parseDouble(params[i]));
                                path.lineTo((float) currentPoint.getX(), (float) currentPoint.getY());
                            }
                            break;
                            
                        case 'v':
                            for(i = 1; params.length - i > 0; i++){
                                currentPoint = new Point2D.Double(  currentPoint.getX(),
                                                                    currentPoint.getY() + Double.parseDouble(params[i]));
                                path.lineTo((float) currentPoint.getX(), (float) currentPoint.getY());
                            }
                            break;
                            
                        case 'Z':
                        case 'z':
                            break;
                        default:
                            throw new RuntimeException("attribute not recognized: "+params[0].charAt(0));
                    }
                    
                }
                return CommandBuilder.shape(path);
            }
        });
        
        builders.put("polygon", new ParserBuilder() {
            public Command build(String... parts) {
                GeneralPath path = buildPolyline(parts);
                path.closePath();
                return CommandBuilder.shape(path);
            }
        });
        
        builders.put("polyline", new ParserBuilder() {
            public Command build(String... parts) {
                GeneralPath path = buildPolyline(parts);
                return CommandBuilder.shape(path);
            }
        });
        
        builders.put("pop", new ParserBuilder() {
            public Command build(String... parts) {
                String type = parts[1];
                
                if(type.equals("clip-path")){
                    // TODO: Implement.
                    throw new RuntimeException("unknown pop type: clip-path");
                }
              
                if(type.equals("gradient")){
                    // TODO: Implement.
                    throw new RuntimeException("unknown pop type: gradient");
                }
                
                if(type.equals("pattern")){
                    return CommandBuilder.nil();
                }
              
                if(type.equals("graphic-context")) {
                    return CommandBuilder.pop();
                }
                
                if(type.equals("defs")){
                    return CommandBuilder.nil(); // Yep, it does nothing.
                }
                
                throw new RuntimeException("unknown pop type: " + type);
            }
        });
        
        builders.put("push", new ParserBuilder() {
            public Command build(String... parts) {
                String type = parts[1];
                
                if(type.equals("clip-path")){
                    // TODO: Implement.
                    throw new RuntimeException("unknown push type: clip-path");
                }
              
                if(type.equals("gradient")){
                    // TODO: Implement.
                    throw new RuntimeException("unknown push type: gradient");
                }
                
                if(type.equals("pattern")){
                    String name = parts[2];
                    int x = Integer.parseInt(parts[3]);
                    int y = Integer.parseInt(parts[4]);
                    int width = Integer.parseInt(parts[5]);
                    int height = Integer.parseInt(parts[6]);
                    
                    Pattern pattern = new Pattern(name, x, y, width, height);
                    return CommandBuilder.pushPattern(pattern);
                }
              
                if(type.equals("graphic-context")) {
                    return CommandBuilder.push();
                }
                
                if(type.equals("defs")){
                    return CommandBuilder.nil(); // Yep, it does nothing.
                }
                
                throw new RuntimeException("unknown push type: " + type);
            }
        });
        
        builders.put("rectangle", new ParserBuilder() {
            public Command build(String... parts) {
                String[] args0 = parts[1].split(",");
                String[] args1 = parts[2].split(",");
                double x1 = Double.parseDouble(args0[0]);
                double y1 = Double.parseDouble(args0[1]);
                double x2 = Double.parseDouble(args1[0]);
                double y2 = Double.parseDouble(args1[1]);
                return CommandBuilder.shape(new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1)));
            }
        });
        
        builders.put("roundrectangle", new ParserBuilder() {
            public Command build(String... parts) {
                String[] args = parts[1].split(",");
                double x1 = Double.parseDouble(args[0]);
                double y1 = Double.parseDouble(args[1]);
                double x2 = Double.parseDouble(args[2]);
                double y2 = Double.parseDouble(args[3]);
                double cornerWidth = Double.parseDouble(args[4]);
                double cornerHeight = Double.parseDouble(args[5]);
                return CommandBuilder.shape(new RoundRectangle2D.Double(x1, y1, x2 - x1, y2 - y1, cornerWidth, cornerHeight));
            }
        });
        
        builders.put("scale", new ParserBuilder() {
            public Command build(String... parts) {
                String[] args = parts[1].split(",");
                return CommandBuilder.scale(Double.parseDouble(args[0]), Double.parseDouble(args[1]));
            }
        });
        
        builders.put("stroke", new ParserBuilder() {
            public Command build(String... parts) {
                String colorName = parts[1].replace("\"", "");
                
                return CommandBuilder.stroke(colorName);
            }
        });
        
        builders.put("stroke-antialias", new ParserBuilder() {
            public Command build(String... parts) {
                return CommandBuilder.strokeAntialias(Integer.parseInt(parts[1]) == 1);
            }
        });
        
        builders.put("stroke-dasharray", new ParserBuilder() {
            public Command build(String... parts) {
                String[] args = parts[1].split(",");
                double[] lengths = new double[args.length];
                for (int a = 0; a < args.length; a++) {
                    lengths[a] = Double.parseDouble(args[a]);
                }
                return CommandBuilder.strokeDashArray(lengths);
            }
        });
        
        builders.put("stroke-opacity", new ParserBuilder() {
            public Command build(String... parts) {
                // Manage two different type of string:
                // 1.- "0.3"
                // 2.- "30%"
                String opacity = parts[1];
                double value = 0.0;
                char[] string = opacity.toCharArray();
                if(string[string.length-1] == '%'){
                    opacity = opacity.substring(0, string.length-1);
                    value = Double.parseDouble(opacity)/100.0;
                }else{
                    value = Double.parseDouble(opacity);
                }
                return CommandBuilder.strokeOpacity(value);
            }
        });
        
        builders.put("stroke-width", new ParserBuilder() {
            public Command build(String... parts) {
                return CommandBuilder.strokeWidth(Double.parseDouble(parts[1]));
            }
        });
        
        return builders;
    }

    private static GeneralPath buildPolyline(String parts[]) {
        GeneralPath path = new GeneralPath();
        String[] args = parts[1].split(",");
        for (int a = 0; a < args.length; a += 2) {
            double x = Double.parseDouble(args[a]);
            double y = Double.parseDouble(args[a + 1]);
            if (a == 0) {
                path.moveTo((float) x, (float) y);
            } else {
                path.lineTo((float) x, (float) y);
            }
        }
        return path;
    }

    public static List<Command> parse(String script) {
        try {
            List<Command> commands = new ArrayList<Command>();
            BufferedReader reader = new BufferedReader(new StringReader(script));
            // TODO MVG specs say this isn't really line based. They could all be on one line.
            String line;
            while ((line = reader.readLine()) != null) {
                commands.add(parseCommand(line));
            }
            // Officially no need to close StringReaders.
            return commands;
        } catch (Exception e) {
            throw Thrower.throwAny(e);
        }
    }

    private static Command parseCommand(String text) {
        String[] parts = text.split(" +");
        String command = parts[0];
        ParserBuilder builder = BUILDERS.get(command);
        if (builder == null) {
            // TODO This should also be the error for bad params (at least when I tested roundrectangle with only 2)
            // Magick::ImageMagickError: Non-conforming drawing primitive definition `yodle'
            // irb(main):018:0> Magick::ImageMagickError.superclass
            // => StandardError
            // TODO Also, this text might ought to be in the Ruby side, not in the Java side.
            throw new RuntimeException("Non-conforming drawing primitive definition `" + command + "'");
        }
        return builder.build(parts);
    }
}
