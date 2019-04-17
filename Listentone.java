package com.example.sound.devicesound;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.*;

import java.util.ArrayList;
import java.util.List;

public class Listentone {

    int HANDSHAKE_START_HZ = 4096;
    int HANDSHAKE_END_HZ = 5120 + 1024;

    int START_HZ = 1024;
    int STEP_HZ = 256;
    int BITS = 4;

    int FEC_BYTES = 4;

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = 44100;
    private int mChannelCount = AudioFormat.CHANNEL_IN_MONO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private float interval = 0.1f;

    private int mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelCount, mAudioFormat);//num_frames

    public AudioRecord mAudioRecord = null;
    int audioEncodig;
    boolean startFlag;
    FastFourierTransformer transform;



    public Listentone(){

        transform = new FastFourierTransformer(DftNormalization.STANDARD);
        startFlag = false;
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelCount, mAudioFormat, mBufferSize);
        mAudioRecord.startRecording();


    }

    int findPowerSize(int bfsize)
    {
        for(int i=1;i<99;i++)
        {
            if(bfsize<2)return 2;
            if(Math.pow(2,i)<=bfsize && bfsize<Math.pow(2,i+1))
            {
                double between=(Math.pow(2,i)+bfsize+Math.pow(2,i+1))/2;
                if(bfsize<=between)
                {
                    return (int)Math.pow(2,i);
                }
                else return  (int)Math.pow(2,i+1)/2;
            }
        }
        return 0;
    }

    void PreRequest()
    {
        boolean in_packet=false;
        int num=0,packet_size=0;
        double f;
        double[] packet= new double[99];
        Log.d("ListenTone","play");
        while(true) {
            int blocksize = findPowerSize((int) (long) Math.round(interval / 2 * mSampleRate));
            short[] buffer = new short[blocksize];
            double[] chunk= new double[blocksize];
            int bufferedReadResult = mAudioRecord.read(buffer, 0, blocksize);
            int[] result;
            char[] A;
            String lastre;
            int[] arch;
            for(int i=0;i<blocksize;i++) {
                chunk[i]=buffer[i];
            }
            f=(int)findFrequencey(chunk);
            Log.d("ListenTone",Double.toString(f));
            if(in_packet&& Math.abs(f-HANDSHAKE_END_HZ)<20)
            {
                Log.d("ListenTone","END");
                packet_size=num+1-18;

                result=new int[packet_size/2];
                num=0;
                in_packet=false;

                int[ ] bit_chunk=new int[packet_size];
                for(int i=2;i<packet_size;i++)
                {
                    if(i%2==0) {
                        bit_chunk[i] = (int) (Math.round((packet[i] - START_HZ) / STEP_HZ));
                        result[(i - 2)/2] = bit_chunk[i];
                        Log.d("ListenTone bit_Chunk", "chunk: " + Integer.toString(result[(i - 2)/2]));
                    }

                }
                arch=new int[result.length/2];
                A=new char[result.length/2];
                for(int i=0;i<result.length-1;i++)
                {
                    if(i%2==0)
                    {
                        arch[i/2]=result[i]*16+result[i+1];

                        Log.d("ListenTone bit_Chunk", "arch: " + Integer.toString(arch[i/2]));
                        A[i/2]=(char)arch[i/2];
                        Log.d("ListenTone bit_Chunk", "arschii: " + A[i/2]);

                    }

                }

                lastre=new String(A);
                Log.d("ListenTone bit_Chunk","result:"+lastre);
            }
            else if(in_packet)
            {
                Log.d("ListenTone",Double.toString(f));
                packet[num++]=f;

            }
            else if(Math.abs(f-HANDSHAKE_START_HZ)<20)
            {
                Log.d("ListenTone","START");
                Log.d("ListenTone",Double.toString(f));
                in_packet=true;
            }

        }


    }

    private double findFrequencey(double [] toTransform)
    {
        int len = toTransform.length;
        double[] real= new double[len];
        double[] img = new double[len];
        double realNum;
        double imgNum;
        double[] mag = new double[len];
        double max=0;
        double f;
        int index=0;
        Complex[] complx = transform.transform(toTransform,TransformType.FORWARD);

        Double[] freq =this.fftfreq(complx.length,1);

        for(int i=0;i<complx.length;i++)
        {
            realNum=complx[i].getReal();//실수
            imgNum=complx[i].getImaginary();//허수
            mag[i] = Math.sqrt((realNum*realNum)+(imgNum*imgNum));//제곱의 합의 제곱합
            if(mag[i]<0)
            {
                mag[i]=-mag[i];
            }
            if(max<mag[i]) {
                max = mag[i];
                index=i;
            }
        }

        //Log.d("ListenTone",Double.toString(max));

        f=freq[index]*mSampleRate;
        if(f<0)f=-f;

        //Log.d("freq",Double.toString(f));



        return f;

    }
    Double[] fftfreq(int length, int n)
    {
        double num=0;
        Double[] freq= new Double[length];
        if(length%2==0) {
            for (int i = 0; i < length/2; i++) {
                freq[i] = (double)i;
            }
            num=length/2;
            for (int i = length/2; i <length; i++) {
                freq[i] = -num--;
            }
            for(int i=0;i<length;i++)
            {
                freq[i]=freq[i]/(n*length);
            }
        }
        else if (length%2==1){
            for (int i = 0; i <= (length-1)/2; i++) {
                freq[i] = (double)i;
            }
            num=(length-1)/2;
            for (int i = (length-1)/2+1; i <length; i++) {
                freq[i] = -num--;
            }
            for(int i=0;i<length;i++)
            {
                freq[i]=freq[i]/(n*length);
            }

        }
        return freq;
    }

}
