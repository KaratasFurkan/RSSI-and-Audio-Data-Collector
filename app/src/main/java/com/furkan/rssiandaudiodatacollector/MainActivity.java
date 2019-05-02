package com.furkan.rssiandaudiodatacollector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private String gsmSignalStrength;
    private CountDownTimer intervalTimer;
    private CountDownTimer recurrencyTimer;
    private CountDownTimer audioIntervalTimer;
    private CountDownTimer audioRecurrencyTimer;
    private int millis;
    private int interval;
    private int recurrency;
    private int audioInterval;
    private int audioRecurrency;
    private String date;
    private String fileName;
    private int version = 1;
    private File dataFile;
    private ArrayList<String> hertz = new ArrayList<String>(Arrays.asList("custom", "1", "20", "40", "60", "80", "100", "120"));
    private ArrayList<String> sampleRates = new ArrayList<String>(Arrays.asList("custom", "44100"));
    private ArrayList<String> intervals = new ArrayList<String>(Arrays.asList("custom", "5", "10", "20", "40", "60"));
    private ArrayList<String> audioIntervals = new ArrayList<String>(Arrays.asList("custom", "10", "20", "40", "60"));
    private ArrayList<String> recurrencies = new ArrayList<String>(Arrays.asList("custom", "20", "30", "40", "60", "90", "120", "300"));
    private ArrayList<String> audioRecurrencies = new ArrayList<String>(Arrays.asList("custom", "20", "30", "40", "60", "90", "120", "300"));
    private ArrayList<String> transportModes = new ArrayList<String>(Arrays.asList("walk", "still", "run", "metrobus", "bicycle",
            "train", "motor", "car", "metro", "bus", "ferry", "minibus", "marmaray", "tram"));
    private String transportMode;

    //Sound recorder variables
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String SAVE_FOLDER = "RSSI and Audio Collector";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    //Spinners

    ArrayAdapter<String> hertzAdapter;
    ArrayAdapter<String> tModeAdapter;
    ArrayAdapter<String> recurrencyAdapter;
    ArrayAdapter<String> intervalAdapter;
    ArrayAdapter<String> sampleRateAdapter;
    ArrayAdapter<String> audioIntervalAdapter;
    ArrayAdapter<String> audioRecurrencyAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button startButton = (Button) findViewById(R.id.startButton);
        final Button stopButton = (Button) findViewById(R.id.stopButton);
        final Button applyButton = (Button) findViewById(R.id.applyButton);
        final EditText customInputText = (EditText) findViewById(R.id.customInputText);
        final Spinner hertzSpinner = (Spinner) findViewById(R.id.hertzSpinner);
        final Spinner tModeSpinner = (Spinner) findViewById(R.id.tModeSpinner);
        final Spinner recurrencySpinner = (Spinner) findViewById(R.id.recurrencySpinner);
        final Spinner intervalSpinner = (Spinner) findViewById(R.id.intervalSpinner);
        final Spinner sampleRateSpinner = (Spinner) findViewById(R.id.sampleRateSpinner);
        final Spinner audioRecurrencySpinner = (Spinner) findViewById(R.id.audioRecurrencySpinner);
        final Spinner audioIntervalSpinner = (Spinner) findViewById(R.id.audioIntervalSpinner);

        stopButton.setEnabled(false);

        //Audio
        bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);


        //Buttons
        startButton.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View view){
                        if(isExternalStorageWritable()) {
                            startButton.setEnabled(false);
                            stopButton.setEnabled(true);
                            Toast.makeText(getApplicationContext(), "Started", Toast.LENGTH_SHORT).show();
                            fileName = createFileName();
                            dataFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + SAVE_FOLDER,
                                    fileName+ ".txt");

//

                            try{
                                FileOutputStream fileOS = new FileOutputStream(dataFile, true);
                                fileOS.write((( "RSSI recurrency: " + recurrency / 1000 + " interval: " + interval / 1000
                                        + " hertz: " + (int) Math.pow((double) millis / 1000, -1) + "\n"
                                        + "Audio recurrency: " + audioRecurrency / 1000 + " interval: " + audioInterval / 1000
                                        + " sample rate: " + RECORDER_SAMPLERATE +"\n").getBytes()));
                                fileOS.close();
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                            recurrencyTimer = new CountDownTimer(999999999, recurrency - 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    intervalTimer = new CountDownTimer(interval + 1000, millis) {
                                        @Override
                                        public void onTick(long millisUntilFinished) {
                                            try{
                                                FileOutputStream fileOS = new FileOutputStream(dataFile, true);
                                                fileOS.write((( new SimpleDateFormat("HH-mm-ss_dd-MM-yyyy", Locale.getDefault()).format(new Date()) + "," + getTimeStamp() + "," +  gsmSignalStrength + "," + transportMode
                                                        + "\n").getBytes()));
                                                fileOS.close();
                                            }catch (IOException e){
                                                e.printStackTrace();
                                            }
                                        }
                                        @Override
                                        public void onFinish() { }
                                    }.start();
                                }
                                @Override
                                public void onFinish() {}
                            }.start();

                            audioRecurrencyTimer = new CountDownTimer(999999999, audioRecurrency - 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    startRecording();
                                    audioIntervalTimer = new CountDownTimer(audioInterval + 1000, millis) {
                                        @Override
                                        public void onTick(long millisUntilFinished) { }
                                        @Override
                                        public void onFinish() {
                                            stopRecording();
                                        }
                                    }.start();
                                }
                                @Override
                                public void onFinish() {}
                            }.start();
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "Cannot write to External Storage.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        stopButton.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View view){
                        Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
                        intervalTimer.cancel();
                        recurrencyTimer.cancel();
                        audioIntervalTimer.cancel();
                        audioRecurrencyTimer.cancel();
                        stopRecording();
                        stopButton.setEnabled(false);
                        startButton.setEnabled(true);
                    }
                }
        );

        //SignalStrength
        final TelephonyManager mFlags;
        mFlags = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            public void onSignalStrengthsChanged (SignalStrength signalStrength)
            {
                super.onSignalStrengthsChanged(signalStrength);
                if ( mFlags.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE){
                    gsmSignalStrength = String.valueOf("Lte," + signalStrength.toString().split(" ")[8]);
                }
                else{
                    gsmSignalStrength = String.valueOf("GSM," + signalStrength.getGsmSignalStrength());
                }
            }
        };
        mFlags.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        //Spinners
        hertzAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, hertz);
        tModeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, transportModes);
        recurrencyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, recurrencies);
        intervalAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, intervals);
        sampleRateAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sampleRates);
        audioIntervalAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, audioIntervals);
        audioRecurrencyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, audioRecurrencies);

        hertzAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recurrencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sampleRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        audioIntervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        audioRecurrencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        hertzSpinner.setAdapter(hertzAdapter);                      hertzSpinner.setSelection(1);
        tModeSpinner.setAdapter(tModeAdapter);                      //there is no custom option here
        recurrencySpinner.setAdapter(recurrencyAdapter);            recurrencySpinner.setSelection(1);
        intervalSpinner.setAdapter(intervalAdapter);                intervalSpinner.setSelection(1);
        sampleRateSpinner.setAdapter(sampleRateAdapter);            sampleRateSpinner.setSelection(1);
        audioRecurrencySpinner.setAdapter(audioRecurrencyAdapter);  audioRecurrencySpinner.setSelection(1);
        audioIntervalSpinner.setAdapter(audioIntervalAdapter);      audioIntervalSpinner.setSelection(1);


        hertzSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if(adapterView.getSelectedItem().toString().matches("custom")){
                            customInputText.setText("");
                            customInputText.setVisibility(View.VISIBLE);
                            applyButton.setVisibility(View.VISIBLE);
                            applyButton.setOnClickListener(
                                    new Button.OnClickListener(){
                                        public void onClick(View view){
                                            hertz.add(customInputText.getText().toString());
                                            hertzSpinner.setSelection(hertzAdapter.getPosition(customInputText.getText().toString()));
                                            customInputText.setVisibility((View.INVISIBLE));
                                            applyButton.setVisibility((View.INVISIBLE));
                                            Toast.makeText(getApplicationContext(), customInputText.getText().toString() + " applied and added to list", Toast.LENGTH_SHORT).show();
                                            millis = Integer.parseInt(customInputText.getText().toString()); //hertz
                                            millis = 1000 / millis; //milliseconds
                                        }
                                    }
                            );
                        }
                        else{
                            millis = Integer.parseInt(adapterView.getSelectedItem().toString()); //hertz
                            millis = 1000 / millis; //milliseconds
                            customInputText.setVisibility(View.INVISIBLE);
                            applyButton.setVisibility(View.INVISIBLE);
                        }
                    }


                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        millis = Integer.parseInt(adapterView.getItemAtPosition(1).toString()); //hertz
                        millis = 1000 / millis; //milliseconds
                    }
                }
        );

        tModeSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        transportMode = adapterView.getSelectedItem().toString();
                        customInputText.setVisibility(View.INVISIBLE);
                        applyButton.setVisibility(View.INVISIBLE);

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        transportMode = adapterView.getItemAtPosition(1).toString();
                    }
                }
        );

        recurrencySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if(adapterView.getSelectedItem().toString().matches("custom")){
                            customInputText.setText("");
                            customInputText.setVisibility(View.VISIBLE);
                            applyButton.setVisibility(View.VISIBLE);
                            applyButton.setOnClickListener(
                                    new Button.OnClickListener(){
                                        public void onClick(View view){
                                            recurrencies.add(customInputText.getText().toString());
                                            recurrencySpinner.setSelection(recurrencyAdapter.getPosition(customInputText.getText().toString()));
                                            Toast.makeText(getApplicationContext(), customInputText.getText().toString() + " applied and added to list", Toast.LENGTH_SHORT).show();
                                            recurrency = Integer.parseInt(customInputText.getText().toString()) * 1000;
                                        }
                                    }
                            );
                        }
                        else{
                            recurrency = Integer.parseInt(adapterView.getSelectedItem().toString()) * 1000;
                            customInputText.setVisibility(View.INVISIBLE);
                            applyButton.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        recurrency = Integer.parseInt(adapterView.getItemAtPosition(1).toString()) * 1000;
                    }
                }
        );

        intervalSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if(adapterView.getSelectedItem().toString().matches("custom")){
                            customInputText.setText("");
                            customInputText.setVisibility(View.VISIBLE);
                            applyButton.setVisibility(View.VISIBLE);
                            applyButton.setOnClickListener(
                                    new Button.OnClickListener(){
                                        public void onClick(View view){
                                            intervals.add(customInputText.getText().toString());
                                            intervalSpinner.setSelection(intervalAdapter.getPosition(customInputText.getText().toString()));
                                            Toast.makeText(getApplicationContext(), customInputText.getText().toString() + " applied and added to list", Toast.LENGTH_SHORT).show();
                                            interval = Integer.parseInt(customInputText.getText().toString()) * 1000;
                                        }
                                    }
                            );
                        }
                        else{
                            interval = Integer.parseInt(adapterView.getSelectedItem().toString()) * 1000;
                            customInputText.setVisibility(View.INVISIBLE);
                            applyButton.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        interval = Integer.parseInt(adapterView.getItemAtPosition(1).toString()) * 1000;
                    }
                }
        );

        audioRecurrencySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if(adapterView.getSelectedItem().toString().matches("custom")){
                            customInputText.setText("");
                            customInputText.setVisibility(View.VISIBLE);
                            applyButton.setVisibility(View.VISIBLE);
                            applyButton.setOnClickListener(
                                    new Button.OnClickListener(){
                                        public void onClick(View view){
                                            audioRecurrencies.add(customInputText.getText().toString());
                                            audioRecurrencySpinner.setSelection(audioRecurrencyAdapter.getPosition(customInputText.getText().toString()));
                                            Toast.makeText(getApplicationContext(), customInputText.getText().toString() + " applied and added to list", Toast.LENGTH_SHORT).show();
                                            audioRecurrency = Integer.parseInt(customInputText.getText().toString()) * 1000;
                                        }
                                    }
                            );
                        }
                        else{
                            audioRecurrency = Integer.parseInt(adapterView.getSelectedItem().toString()) * 1000;
                            customInputText.setVisibility(View.INVISIBLE);
                            applyButton.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        audioRecurrency = Integer.parseInt(adapterView.getItemAtPosition(1).toString()) * 1000;
                    }
                }
        );

        audioIntervalSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if(adapterView.getSelectedItem().toString().matches("custom")){
                            customInputText.setText("");
                            customInputText.setVisibility(View.VISIBLE);
                            applyButton.setVisibility(View.VISIBLE);
                            applyButton.setOnClickListener(
                                    new Button.OnClickListener(){
                                        public void onClick(View view){
                                            audioIntervals.add(customInputText.getText().toString());
                                            audioIntervalSpinner.setSelection(audioIntervalAdapter.getPosition(customInputText.getText().toString()));
                                            Toast.makeText(getApplicationContext(), customInputText.getText().toString() + " applied and added to list", Toast.LENGTH_SHORT).show();
                                            audioInterval = Integer.parseInt(customInputText.getText().toString()) * 1000;
                                        }
                                    }
                            );
                        }
                        else{
                            audioInterval = Integer.parseInt(adapterView.getSelectedItem().toString()) * 1000;
                            customInputText.setVisibility(View.INVISIBLE);
                            applyButton.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        audioInterval = Integer.parseInt(adapterView.getItemAtPosition(1).toString()) * 1000;
                    }
                }
        );

        sampleRateSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if(adapterView.getSelectedItem().toString().matches("custom")){
                            customInputText.setText("");
                            customInputText.setVisibility(View.VISIBLE);
                            applyButton.setVisibility(View.VISIBLE);
                            applyButton.setOnClickListener(
                                    new Button.OnClickListener(){
                                        public void onClick(View view){
                                            sampleRates.add(customInputText.getText().toString());
                                            sampleRateSpinner.setSelection(sampleRateAdapter.getPosition(customInputText.getText().toString()));
                                            Toast.makeText(getApplicationContext(), customInputText.getText().toString() + " applied and added to list", Toast.LENGTH_SHORT).show();
                                            RECORDER_SAMPLERATE = Integer.parseInt(customInputText.getText().toString());                                        }
                                    }
                            );
                        }
                        else{
                            RECORDER_SAMPLERATE = Integer.parseInt(adapterView.getSelectedItem().toString());
                            customInputText.setVisibility(View.INVISIBLE);
                            applyButton.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        RECORDER_SAMPLERATE = Integer.parseInt(adapterView.getItemAtPosition(1).toString());
                    }
                }
        );
    }


    //FUNCTIONS
    public boolean isExternalStorageWritable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return true;
        }
        return false;
    }

    public long getTimeStamp(){
        return System.currentTimeMillis();
    }

    //Audio
    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void startRecording(){
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        int i = recorder.getState();
        if(i==1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

    private void stopRecording(){
        if(null != recorder){
            isRecording = false;

            int i = recorder.getState();
            if(i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(),getFilename());
        deleteTempFile();
    }

    private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read = 0;

        if(null != os){
            while(isRecording){
                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,SAVE_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + fileName + "_" + version++ + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,SAVE_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    public String createFileName(){
        date = new SimpleDateFormat("HH-mm-ss_dd-MM-yyyy", Locale.getDefault()).format(new Date());
        return date + "__" + getTimeStamp();
    }
}