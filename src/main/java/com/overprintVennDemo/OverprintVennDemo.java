package com.overprintVennDemo;

import com.pdftron.pdf.*;
import com.pdftron.sdf.SDFDoc;
import com.pdftron.sdf.Obj;

import io.github.cdimascio.dotenv.Dotenv;

public class OverprintVennDemo {

    static void createMoveTo(double x, double y, StringBuilder sb) {
        sb.append(x).append(' ').append(y).append(" m\n");
    } 

    static void createCurveTo(double x1, double y1, double x2, double y2, double x3, double y3, StringBuilder sb) {
        // x1, y1, x2, y2, x3, y3 c
        sb.append(x1).append(' ')
        .append(y1).append(' ')
        .append(x2).append(' ')
        .append(y2).append(' ')
        .append(x3).append(' ')
        .append(y3)
        .append(" c\n");
        //  c = PDF path operator curveTo. 
        // 1. "Start coordinates, control point pulling curve as it leaves start"
        // 2. "End coordinates, control point pulling curve as it approaches end"
        // 3. Ending point of curve
        // Syntax in PDF: x1, y1, x2, y2, x3, y3 c
    }

    static double mmToUnits(double mm) {
    return mm / 25.4 * 72.0;
    }

    static double widthA4PortraitInMM = 210;
    static double heightA4PortraitInMM = 297;
    static double widthA4PortraitInPoints = mmToUnits(widthA4PortraitInMM);
    static double heightA4PortraitInPoints = mmToUnits(heightA4PortraitInMM);

    public static void makeDoc(boolean overprint, String outPath) throws Exception {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        String token = dotenv.get("PDFTRON_KEY");

        PDFNet.initialize(token);
        PDFDoc doc = new PDFDoc();

        // Create your first page and mediabox before beginning to lay out content
        Page page = doc.pageCreate(new Rect(0, 0, widthA4PortraitInPoints, heightA4PortraitInPoints)); 
        doc.pagePushBack(page);
        
        double pageWidth = page.getPageWidth();
        double pageHeight = page.getPageHeight();
        double pageCentreVertically = pageWidth / 2.0;
        double pageCentreHorizontally = pageHeight / 2.0;
        double radiusOfCircle = Math.min(pageWidth, pageHeight) * 0.2;
        double circleOffset = radiusOfCircle * 0.4;

        Obj pageDict = page.getSDFObj();
        Obj resources = pageDict.putDict("Resources");
        Obj extendedGraphicsStateDictionary = resources.putDict("ExtGState");
        Obj graphicStateForCircleDrawing = extendedGraphicsStateDictionary.putDict("graphicStateForCircleDrawing");

        graphicStateForCircleDrawing.putName("Type", "ExtGState");
        graphicStateForCircleDrawing.putBool("OP", overprint); 
        // Set to overprint mode 
        graphicStateForCircleDrawing.putNumber("OPM", overprint ? 1 : 0);
        // Decide to apply overprint strictly:
            // With 1, full overprint
            // With 0, some knockout still takes place: that of 0-valued objects

        StringBuilder sb = new StringBuilder();

        sb.append("1 1 1 rg 1 1 1 RG 0 0 ") // rg = non-stroking RGB, RG = stroking RGB. 1 1 1 = white, 0 0 = starting x, y
        .append(pageWidth) // <pageWidth> 
        .append(' ')
        .append(pageHeight) // <pageHeight>
        .append(" re f\n"); // re = rectangle path operator

        // Use method below, appendCircle 
        // Use string builder you have started, sb 
        // Have the circle offset based on whether it's the 1rst or 2nd one, (pageCentreVertically - circleOffset) or (pageCentreVertically + circleOffset)
        // Both vertically laid the same way, pageCentreHorizontally
        // The radius of the circle you would like to have, radiusOfCircle
        // "1 0 0 0 k\n" for cyan, "0 1 0 0 k\n" for magenta, CMYK colour operators

        appendCircle(sb, pageCentreVertically - circleOffset, pageCentreHorizontally, radiusOfCircle, "1 0 0 0 k\n");

        sb.append("/graphicStateForCircleDrawing gs\n");

        appendCircle(sb, pageCentreVertically + circleOffset, pageCentreHorizontally, radiusOfCircle, "0 1 0 0 k\n");

        Obj contents = doc.createIndirectStream(sb.toString().getBytes("UTF-8"));
        
        pageDict.put("Contents", contents);

        doc.save(outPath, SDFDoc.SaveMode.LINEARIZED, null);
        doc.close();
    }

    // Append a Bezier-approximated circle path with fill color line included.
    public static void appendCircle(StringBuilder sb, double pageCentreVertically, double pageCentreHorizontally,
            double radiusOfCircle, String colorLine) {

        // We are drawing a circle. Think of each 90° point as the points on an analog clock.
        double xCoordinate12h = pageCentreVertically;
        double yCoordinate12h = pageCentreHorizontally + radiusOfCircle;
        double xCoordinate3h = pageCentreVertically + radiusOfCircle;
        double yCoordinate3h = pageCentreHorizontally;
        double xCoordinate6h = pageCentreVertically;
        double yCoordinate6h = pageCentreHorizontally - radiusOfCircle;
        double xCoordinate9h = pageCentreVertically - radiusOfCircle;
        double yCoordinate9h = pageCentreHorizontally;
        // See demo/src/main/resources/diagram_plot_points.jpg
        
        double kappa = 0.552284749831;
        double offsetBeforePlacingCtrlPt = radiusOfCircle * kappa;

        // kappa is the fraction of the radius you move the Bezier control points away from each 90° point.
        // That way, the four cubic Beziers look almost exactly like a true circle.
        // https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Properties, 4(√2 − 1) / 3
        // see demo/src/main/resources/approximate_circle_with_bezier_curve.png

        // Set CMYK fill color
        sb.append(colorLine);

        // Top
        createMoveTo(xCoordinate12h, yCoordinate12h, sb);
        //  m = PDF path operator moveTo. "Move the starting point to the specific coordinates"

        // Top
        createCurveTo(
                xCoordinate12h + offsetBeforePlacingCtrlPt, 
                yCoordinate12h, 
                xCoordinate3h, 
                yCoordinate3h + offsetBeforePlacingCtrlPt, 
                xCoordinate3h, 
                yCoordinate3h, 
                sb);
                
        // Bottom-right
        createCurveTo(
                xCoordinate3h, 
                yCoordinate3h - offsetBeforePlacingCtrlPt, 
                xCoordinate6h + offsetBeforePlacingCtrlPt, 
                yCoordinate6h, 
                xCoordinate6h, 
                yCoordinate6h, 
                sb);

        // Bottom-left
        createCurveTo(
                xCoordinate6h - offsetBeforePlacingCtrlPt, 
                yCoordinate6h, 
                xCoordinate9h, 
                yCoordinate9h - offsetBeforePlacingCtrlPt, 
                xCoordinate9h, 
                yCoordinate9h, 
                sb);

        // Top-left
        createCurveTo(
                xCoordinate9h, 
                yCoordinate9h + offsetBeforePlacingCtrlPt, 
                xCoordinate12h - offsetBeforePlacingCtrlPt, 
                yCoordinate12h, 
                xCoordinate12h, 
                yCoordinate12h, 
                sb);

        // Fill the path
        sb.append("f\n");
        // f = fill path interior
    }
}