package net.theodr;

import ar.com.hjg.pngj.*;
import ar.com.hjg.pngj.chunks.*;
import ar.com.hjg.pngj.pixels.CompressorStreamParallelDeflater;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * APNG Disassembler
 * 
 * An imitation of Max Stepin’s apngdis, but faster (because it’s Java)
 * and multi-threaded. And more color-accurate.
 */
public final class ApngDis {

    private static final ChunkPredicate COPYPOLICY = new ChunkPredicate(){
        public boolean match(PngChunk chunk) {
            if (chunk.safe) return true; // Safe chunks such as eXIf.
            switch(chunk.id) {
                case ChunkHelper.PLTE: // Palette for indexed color PNG
                case ChunkHelper.tRNS: // Transparent pixels for either indexed or color
                case ChunkHelper.bKGD: // Background color if no pixels are rendered
                case ChunkHelper.gAMA: // Gamma
                case ChunkHelper.iCCP: // ICC Color Profile
                case ChunkHelper.cHRM: // Chromaticity coordinates
                case ChunkHelper.sBIT: // Significant bits
                case ChunkHelper.sPLT: // Palette to use if true color is unavailable
                case ChunkHelper.sRGB: // sRGB rendering intent
                case "sTER": // TODO: Add support for stereo to the dependency library.
                case "sCAL": // TODO: Add support for physical scale to the dependency library.
                case "pCAL": // TODO: Add support for physical measurements to the dependency library.
                return true;
                default:
                return false;
            }
        }
    };

    public static void process(PngReaderApng pngr, File parent, String prefix) throws Exception {
        int numFrames = pngr.getApngNumFrames();
        int digits = (int) Math.log10(numFrames) + 1;
        String formatString = "%0" + digits + "d";
        PngWriter[] dests = new PngWriter[numFrames];
        for (int i = pngr.hasExtraStillImage() ? -1 : 0 ; i < numFrames; i++) {
            System.out.println("extracting frame " + (i+1) + " of " + numFrames);
            pngr.advanceToFrame(i);
            File dest = new File(parent, prefix + String.format(formatString, i+1) + ".png");
            PngWriter pngw = new PngWriter(dest, pngr.imgInfo, true);
            pngw.copyChunksFrom(pngr.getChunksList(), COPYPOLICY);
            pngw.getPixelsWriter().setDeflaterParallelExecutor(CompressorStreamParallelDeflater.getSharedThreadPool());
            for (int row = 0; row < pngr.imgInfo.rows; row++) {
                pngw.writeRow(pngr.readRow(), row);
            }
            dests[i] = pngw;
            PngChunkFCTL fctlChunk = pngr.getFctl();
            if (fctlChunk != null) {
                FileWriter txtDest = new FileWriter(new File(parent, prefix + String.format(formatString, i+1) + ".txt"));
                BufferedWriter w = new BufferedWriter(txtDest);
                w.write(String.format("delay=%d/%d", fctlChunk.getDelayNum(), fctlChunk.getDelayDen()));
                w.close();
            }
        }
        pngr.end();
        for(int i = 0; i < numFrames; i++) {
            dests[i].end();
        }
    }
    
    private static PngReaderApng loadApng(File orig) throws Exception {
        PngReaderApng pngr = new PngReaderApng(orig);
        if (!pngr.isApng())
            throw new RuntimeException("Not APNG");
        return pngr;
    }
    
    /**
     * Disassembles APNG into PNG
     */
    public static void main(String[] args) throws Exception {
        System.out.println("APNG Disassembler\n");
        if (args.length == 0) {
            System.out.println("Usage: " + ApngDis.class.getSimpleName() + " anim.png [name]");
            System.exit(1);
        }
        File input = new File(args[0]);
        String prefix = args.length > 1 ? args[1] : "apngframe";
        PngReaderApng pngr = null;
        try {
            pngr = loadApng(input);
        } catch (Exception e) {
            System.out.println("load_apng() failed: " + e.getMessage());
            e.printStackTrace(System.out);
            System.exit(1);
        }
        process(pngr, input.getParentFile(), prefix);
        System.out.println("all done");
    }
}
