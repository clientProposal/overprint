package com.demo;

import com.overprintVennDemo.*;
import com.pdftron.pdf.*;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        try {
            OverprintVennDemo.makeDoc(false, "overprint_off.pdf");
            OverprintVennDemo.makeDoc(true, "overprint_on.pdf"); 
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            PDFNet.terminate();
        }
    }
}
