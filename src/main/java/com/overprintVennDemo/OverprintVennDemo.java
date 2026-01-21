package com.overprintVennDemo;

import com.pdftron.pdf.*;
import com.pdftron.sdf.SDFDoc;
import com.pdftron.sdf.Obj;
import com.pdftron.common.*;
import java.util.*;
import io.github.cdimascio.dotenv.Dotenv;

public class OverprintVennDemo {

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
        double pageCentreHorizontally = pageWidth / 2.0;
        double pageCentreVertically = pageHeight / 2.0;
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
        // Have the circle offset based on whether it's the 1rst or 2nd one, (pageCentreHorizontally - circleOffset) or (pageCentreHorizontally + circleOffset)
        // Both vertically laid the same way, pageCentreVertically
        // The radius of the circle you would like to have, radiusOfCircle
        // "1 0 0 0 k\n" for cyan, "0 1 0 0 k\n" for magenta, CMYK colour operators

        appendCircle(sb, pageCentreHorizontally - circleOffset, pageCentreVertically, radiusOfCircle, "1 0 0 0 k\n");

        sb.append("/graphicStateForCircleDrawing gs\n");

        appendCircle(sb, pageCentreHorizontally + circleOffset, pageCentreVertically, radiusOfCircle, "0 1 0 0 k\n");

        Obj contents = doc.createIndirectStream(sb.toString().getBytes("UTF-8"));
        pageDict.put("Contents", contents);

        doc.save(outPath, SDFDoc.SaveMode.LINEARIZED, null);
        doc.close();
    }

    // Append a Bezier-approximated circle path with fill color line included.
    public static void appendCircle(StringBuilder sb, double pageCentreHorizontally, double pageCentreVertically,
            double radiusOfCircle, String colorLine) {

        // We are drawing a circle. Think of each 90° point as the points on an analog clock.
        double xCoordinate12h = pageCentreHorizontally;
        double yCoordinate12h = pageCentreVertically + radiusOfCircle;
        double xCoordinate12_15h = pageCentreHorizontally + radiusOfCircle;
        double yCoordinate12_15h = pageCentreVertically;
        double xCoordinate12_30h = pageCentreHorizontally;
        double yCoordinate12_30h = pageCentreVertically - radiusOfCircle;
        double xCoordinate12_45 = pageCentreHorizontally - radiusOfCircle;
        double yCoordinate12_45 = pageCentreVertically;

        double kappa = 0.5522847498307936;
        double offsetBeforePlacingCtrlPt = radiusOfCircle * kappa;

        // kappa is the fraction of the radius you move the Bezier control points away from each 90° point.
        // That way, the four cubic Beziers look almost exactly like a true circle.
        // https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Properties, 4(√2 − 1) / 3

        // Set CMYK fill color
        sb.append(colorLine);

        // Top
        sb.append(xCoordinate12h).append(' ').append(yCoordinate12h).append(" m\n");
        //  m = PDF path operator moveTo. "Move the starting point to the specific coordinates"

        // Top-right
        sb.append(xCoordinate12h + offsetBeforePlacingCtrlPt).append(' ').append(yCoordinate12h).append(' ')
                .append(xCoordinate12_15h).append(' ').append(yCoordinate12_15h + offsetBeforePlacingCtrlPt).append(' ')
                .append(xCoordinate12_15h).append(' ').append(yCoordinate12_15h).append(" c\n");

        //  c = PDF path operator curveto. 
        // 1. "Start coordinates, control point pulling curve as it leaves start"
        // 2. "End coordinates, control point pulling curve as it approaches end"
        // 3. Ending point of curve
        // Syntax in PDF: x1, y1, x2, y2, x3, y3 c

        // Bottom-right
        sb.append(xCoordinate12_15h).append(' ').append(yCoordinate12_15h - offsetBeforePlacingCtrlPt).append(' ')
                .append(xCoordinate12_30h + offsetBeforePlacingCtrlPt).append(' ').append(yCoordinate12_30h).append(' ')
                .append(xCoordinate12_30h).append(' ').append(yCoordinate12_30h).append(" c\n");

        // Bottom-left
        sb.append(xCoordinate12_30h - offsetBeforePlacingCtrlPt).append(' ').append(yCoordinate12_30h).append(' ')
                .append(xCoordinate12_45).append(' ').append(yCoordinate12_15h - offsetBeforePlacingCtrlPt).append(' ')
                .append(xCoordinate12_45).append(' ').append(yCoordinate12_45).append(" c\n");

        // Top-left
        sb.append(xCoordinate12_45).append(' ').append(yCoordinate12_45 + offsetBeforePlacingCtrlPt).append(' ')
                .append(xCoordinate12h - offsetBeforePlacingCtrlPt).append(' ').append(yCoordinate12h).append(' ')
                .append(xCoordinate12h).append(' ').append(yCoordinate12h).append(" c\n");

        // Fill the path
        sb.append("f\n");
        // f = fill path interior
    }
}