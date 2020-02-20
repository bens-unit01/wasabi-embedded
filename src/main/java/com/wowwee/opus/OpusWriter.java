/**
 * © Copyright IBM Corporation 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.wowwee.opus;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import com.wowwee.audio.AudioFileWriter;
import com.wowwee.audio.IChunkUploader;
import com.wowwee.audio.OggCrc;

import com.wowwee.dto.SpeechConfiguration;

public class OpusWriter extends AudioFileWriter {
    private String TAG = this.getClass().getSimpleName();
    private IChunkUploader uploader;
    /** Number of packets in an Ogg page (must be less than 255) */
    public static final int PACKETS_PER_OGG_PAGE = 5;
    /** Defines the sampling rate of the audio input. */
    protected int sampleRate;
    /** Ogg Stream Serial Number */
    protected int streamSerialNumber;
    /** Data buffer */
    private byte[] dataBuffer;
    /** Pointer within the Data buffer */
    private int dataBufferPtr;
    /** Header buffer */
    private byte[] headerBuffer;
    /** Pointer within the Header buffer */
    private int headerBufferPtr;
    /** Ogg Page count */
    protected int pageCount;
    /** Opus packet count within an Ogg Page */
    private int packetCount;
    /**
     * Absolute granule position
     * (the number of audio samples from beginning of file to end of Ogg Packet).
     */
    private long granulepos;
    /** Frame size */
    private int frameSize;

    public OpusWriter(){}

    /**
     * Setting up the OggOpus Writer
     * @param uploader
     */
    public OpusWriter(IChunkUploader uploader){
        this.uploader = uploader;
        if (streamSerialNumber == 0)
            streamSerialNumber = new Random().nextInt();
        dataBuffer         = new byte[65565];
        dataBufferPtr      = 0;
        headerBuffer       = new byte[255];
        headerBufferPtr    = 0;
        pageCount          = 0;
        packetCount        = 0;
        granulepos         = 0;
        this.sampleRate    = SpeechConfiguration.SAMPLE_RATE;
        this.frameSize     = SpeechConfiguration.FRAME_SIZE;
    }
    @Override
    public void close() throws IOException {
        flush(4);
        this.uploader.stopUpload();
    }

    @Override
    public void open(File file) throws IOException {}

    @Override
    public void open(String filename) throws IOException {}

    @Override
    public void writeHeader(String comment) {
        byte[] header;
        byte[] data;
        byte[] cmtheader;
        byte[] cmtdata;
        int chkSum;
        comment = "";
        /* writes the OGG header page */
        header = buildOggPageHeader(2, 0, streamSerialNumber, pageCount++, 1, new byte[] {19});
        data = buildOpusHeader(sampleRate);
        chkSum = OggCrc.checksum(0, header, 0, header.length);
        chkSum = OggCrc.checksum(chkSum, data, 0, data.length);
        writeInt(header, 22, chkSum);
//        this.write(header);
//        this.write(data);

        /* Writes the OGG comment page */
        cmtheader = buildOggPageHeader(0, 0, streamSerialNumber, pageCount++, 1, new byte[]{24});
        cmtdata = buildOpusComment(comment);
        chkSum = OggCrc.checksum(0, cmtheader, 0, cmtheader.length);
        chkSum = OggCrc.checksum(chkSum, cmtdata, 0, cmtdata.length);
        writeInt(cmtheader, 22, chkSum);
//        this.write(cmtheader);
//      this.write(cmtdata);
        
        ByteBuffer bufferBytes = ByteBuffer.allocate(header.length + data.length+ cmtheader.length+cmtdata.length);
        bufferBytes.put(header);
        bufferBytes.put(data);
        bufferBytes.put(cmtheader);
        bufferBytes.put(cmtdata);        
        byte[] bytes = bufferBytes.array();
        
        this.write(bytes);        
    }

    /**
     * Write data packet
     * @param data audio data
     * @param offset the offset from which to start reading the data.
     * @param len the length of data to read.
     * @throws IOException
     */
    @Override
    public void writePacket(byte[] data, int offset, int len)
            throws IOException {
        // if nothing to write
        if (len <= 0) {
            return;
        }
        int packetIndex = 0;
        while ( len >= 0 ) {
        	if (packetCount >= PACKETS_PER_OGG_PAGE) {
                flush(1);                
            }
            int segmentLength = Math.min( len, 255 );
            System.arraycopy(data, offset, dataBuffer, dataBufferPtr, segmentLength);
            packetIndex += segmentLength;
            dataBufferPtr += segmentLength;            
            headerBuffer[headerBufferPtr++]=(byte) segmentLength;
            packetCount++;
            len -= 255;
        }
        
        granulepos += 960;//this.frameSize*2;
        if (packetCount >= PACKETS_PER_OGG_PAGE) {
            flush(0);
        }
    }

    /**
     * Flush the Ogg page out of the buffers into the file.
     * @param eos - end of stream
     * @exception IOException
     */
    protected void flush(int headerType) throws IOException{
        int chksum;
        byte[] header;
        /* Writes the OGG header page */
        header = buildOggPageHeader(headerType, granulepos, streamSerialNumber, pageCount++, packetCount, headerBuffer);
        chksum = OggCrc.checksum(0, header, 0, header.length);
        chksum = OggCrc.checksum(chksum, dataBuffer, 0, dataBufferPtr);
        writeInt(header, 22, chksum);
        
/*        this.write(header);
        this.write(dataBuffer, 0, dataBufferPtr);*/

        ByteBuffer bufferBytes = ByteBuffer.allocate(header.length + dataBufferPtr);
        bufferBytes.put(header);
        bufferBytes.put(dataBuffer, 0, dataBufferPtr);
        byte[] bytes = bufferBytes.array();
        
        this.write(bytes);

        dataBufferPtr   = 0;
        headerBufferPtr = 0;
        packetCount     = 0;
    }

    /**
     * Write data into Socket
     * @param data
     */
    public void write(byte[] data){
        this.uploader.upload(data);
    }

    /**
     * Write data into Socket
     * @param data
     * @param offset
     * @param count
     */
    public void write(byte[] data, int offset, int count){
        byte[] tmp = new byte[count];
        System.arraycopy(data, offset, tmp, 0, count);

        this.uploader.upload(tmp);
    }
}
