/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.hadeslee.audiotag.audio.flac;

import com.hadeslee.audiotag.audio.exceptions.CannotReadException;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockHeader;
import com.hadeslee.audiotag.audio.flac.metadatablock.MetadataBlockDataPicture;
import com.hadeslee.audiotag.tag.vorbiscomment.VorbisCommentTag;
import com.hadeslee.audiotag.tag.vorbiscomment.VorbisCommentReader;
import com.hadeslee.audiotag.tag.InvalidFrameException;
import com.hadeslee.audiotag.tag.flac.FlacTag;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Read Flac Tag
 */
public class FlacTagReader
{
    // Logger Object
    public static Logger logger = Logger.getLogger("com.hadeslee.jaudiotagger.audio.flac");

    private VorbisCommentReader vorbisCommentReader = new VorbisCommentReader();

    
    public FlacTag read(RandomAccessFile raf) throws CannotReadException, IOException
    {
        FlacStream.findStream(raf);

        //Hold the metadata
        VorbisCommentTag        tag = null;
        List<MetadataBlockDataPicture> images = new ArrayList<MetadataBlockDataPicture>();

        //Seems like we have a valid stream
        boolean isLastBlock = false;
        while (!isLastBlock)
        {
            //Read the header
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);

            //Is it one containing some sort of metadata, therefore interested in it?
            switch (mbh.getBlockType())
            {
                //We got a vorbiscomment comment block, parse it
                case VORBIS_COMMENT :
                    byte[] commentHeaderRawPacket = new byte[mbh.getDataLength()];
                    raf.read(commentHeaderRawPacket);
                    tag = vorbisCommentReader.read(commentHeaderRawPacket,false);                    
                    break;

                case PICTURE:
                    try
                    {
                        MetadataBlockDataPicture mbdp = new MetadataBlockDataPicture(mbh,raf);
                        images.add(mbdp);
                    }
                    catch(IOException ioe)
                    {
                        logger.warning("Unable to read picture metablock, ignoring:"+ioe.getMessage());
                    }
                    catch(InvalidFrameException ive)
                    {
                         logger.warning("Unable to read picture metablock, ignoring"+ive.getMessage());
                    }

                    break;

                //This is not a metadata block we are interested in so we skip to next block
                default :
                    raf.seek(raf.getFilePointer() + mbh.getDataLength());
                    break;
            }

            isLastBlock = mbh.isLastBlock();
            mbh = null;
        }

        //Note there may not be either a tag or any images, no problem this is valid however to make it easier we
        //just initialize Flac with an empty VorbisTag
        if(tag==null)
        {
            tag = new VorbisCommentTag();
        }
        FlacTag flacTag = new FlacTag(tag,images);
        return flacTag;
    }
}

