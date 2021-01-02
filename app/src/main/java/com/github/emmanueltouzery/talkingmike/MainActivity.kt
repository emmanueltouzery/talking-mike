package com.github.emmanueltouzery.talkingmike

import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.media.*
import android.media.AudioTrack
import android.media.audiofx.AcousticEchoCanceler
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.GainProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory


// https://stackoverflow.com/a/27544312/516188
class MainActivity : AppCompatActivity() {

    var isRecording = false
    var am: AudioManager? = null
    //var record: AudioRecord? = null
    var track: AudioTrack? = null
    var gain: Int = 1
    var isChipmunk = false
    var dispatcher: AudioDispatcher? = null
    var gainProcessor: GainProcessor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS), 1)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gainProcessor = GainProcessor(1.0)

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
        // https://github.com/JorenSix/TarsosDSP/blob/master/src/android/be/tarsos/dsp/io/android/AndroidAudioPlayer.java

        volumeControlStream = AudioManager.MODE_IN_COMMUNICATION

        initRecordAndTrack()

        am = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am!!.isSpeakerphoneOn = true

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                gain = progress
                gainProcessor?.setGain((progress+1).toDouble())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        }
        )

        object : Thread() {
            override fun run() {
                recordAndPlay()
            }
        }.start()

        val startButton: Button = findViewById<View>(R.id.button) as Button
        startButton.setOnClickListener {
            if (!isRecording) {
                startRecordAndPlay()
                startButton.text = "Ustavi snemanje"
            } else {
                stopRecordAndPlay()
                startButton.text = "Snemaj"
            }
        }

        val toggleButton = findViewById<Button>(R.id.toggleButton)
        toggleButton.setOnClickListener {
            isChipmunk = !isChipmunk
            resetTrack()
        }
    }

    private fun initRecordAndTrack() {
//        val min = AudioRecord.getMinBufferSize(22050, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
//        record = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 22050, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
//                min)
//        if (AcousticEchoCanceler.isAvailable()) {
//            val echoCanceler = AcousticEchoCanceler.create(record!!.audioSessionId)
//            echoCanceler.enabled = true
//        }
        resetTrack()
    }

    private fun resetTrack() {
        var sampleRate = if (isChipmunk) 44100 else 22050
        val maxJitter = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(maxJitter)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY) //
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

        dispatcher?.addAudioProcessor(gainProcessor)

        dispatcher?.addAudioProcessor(object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                // https://github.com/JorenSix/TarsosDSP/blob/master/src/android/be/tarsos/dsp/io/android/AndroidAudioPlayer.java#L88
                val overlapInSamples: Int = audioEvent.overlap
                val stepSizeInSamples: Int = audioEvent.bufferSize - overlapInSamples
                val byteBuffer: ByteArray = audioEvent.byteBuffer
                Log.i("TAG", "writing samples! " + stepSizeInSamples)

                //int ret = audioTrack.write(audioEvent.getFloatBuffer(),overlapInSamples,stepSizeInSamples,AudioTrack.WRITE_BLOCKING);

                //int ret = audioTrack.write(audioEvent.getFloatBuffer(),overlapInSamples,stepSizeInSamples,AudioTrack.WRITE_BLOCKING);
                val ret: Int = track!!.write(byteBuffer, overlapInSamples * 2, stepSizeInSamples * 2)
                if (ret < 0) {
                    Log.e("TAG", "AudioTrack.write returned error code $ret")
                }
                return true
            }

            override fun processingFinished() {
            }

        })
//        // https://developer.android.com/reference/android/media/AudioFormat#encoding
//        // ENCODING_PCM_16BIT: The audio sample is a 16 bit signed integer typically stored as a Java short in a short array,
//        // but when the short is stored in a ByteBuffer, it is native endian (as compared to the default Java big endian).
//        val isSigned = true
//        val isBigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
//        // https://0110.be/releases/TarsosDSP/TarsosDSP-2.4/TarsosDSP-2.4-Documentation/be/tarsos/dsp/io/TarsosDSPAudioFormat.html#TarsosDSPAudioFormat-float-int-int-boolean-boolean-
//        AndroidAudioPlayer(TarsosDSPAudioFormat(sampleRate as Float, 16, 1, isSigned, isBigEndian))
    }

    private fun recordAndPlay() {
        dispatcher?.run()
    }

    private fun startRecordAndPlay() {
//        record!!.startRecording()
//        track!!.play()
//        isRecording = true
    }

    private fun stopRecordAndPlay() {
//        record!!.stop()
//        track!!.pause()
//        isRecording = false
    }
}