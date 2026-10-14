package com.osrs.combatcoach;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.util.Filepath;
import java.io.InputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Read only a subsampled local thumbnail; caller MUST use the executor, never the game thread. */
public final class CoachThumbnails {
    private CoachThumbnails(){}
    public static BufferedImage load(Filepath file,int maxWidth,int maxHeight)throws IOException{
        if(file==null||!file.isFile()||file.size()>50_000_000L)throw new IOException("Screenshot unavailable");
        try(InputStream stream=file.openInputStream();ImageInputStream input=new MemoryCacheImageInputStream(stream)){
            if(input==null)throw new IOException("Cannot read image");Iterator<ImageReader> readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext())throw new IOException("Unsupported image");ImageReader reader=readers.next();
            try{
                reader.setInput(input,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<1||h<1||w>40000||h>40000)throw new IOException("Unexpected screenshot dimensions");
                ImageReadParam options=reader.getDefaultReadParam();int sub=Math.max(1,Math.min(w/Math.max(1,maxWidth),h/Math.max(1,maxHeight)));
                options.setSourceSubsampling(sub,sub,0,0);BufferedImage sampled=reader.read(0,options);
                double scale=Math.min(1,Math.min(maxWidth/(double)sampled.getWidth(),maxHeight/(double)sampled.getHeight()));
                BufferedImage result=new BufferedImage(Math.max(1,(int)(sampled.getWidth()*scale)),Math.max(1,(int)(sampled.getHeight()*scale)),BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=result.createGraphics();CoachArt.quality(g);g.drawImage(sampled,0,0,result.getWidth(),result.getHeight(),null);g.dispose();return result;
            }finally{reader.dispose();}
        }
    }
}
