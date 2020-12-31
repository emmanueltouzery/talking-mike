package com.github.emmanueltouzery.talkingmike

import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity


// https://stackoverflow.com/a/27544312/516188
class MainActivity : AppCompatActivity() {

    var isRecording = false
    var am: AudioManager? = null
    var record: AudioRecord? = null
    var track: AudioTrack? = null
    var gain: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS), 1)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        volumeControlStream = AudioManager.MODE_IN_COMMUNICATION

        initRecordAndTrack()

        am = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am!!.isSpeakerphoneOn = true

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                gain = progress
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
    }

    private fun initRecordAndTrack() {
        val min = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        record = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                min)
        if (AcousticEchoCanceler.isAvailable()) {
            val echoCanceler = AcousticEchoCanceler.create(record!!.audioSessionId)
            echoCanceler.enabled = true
        }
        val maxJitter = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        track = AudioTrack(AudioManager.MODE_IN_COMMUNICATION, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, maxJitter,
                AudioTrack.MODE_STREAM)
    }

    private fun recordAndPlay() {
        val lin = ShortArray(1024)
        var num = 0
        am!!.mode = AudioManager.MODE_IN_COMMUNICATION

        while (true) {
            if (isRecording) {
                num = record!!.read(lin, 0, 1024)
                transformBytes(lin, num);
                track!!.write(lin, 0, num)
            }
        }
    }

    private fun transformBytes(lin: ShortArray, len: Int) {
        for (i in 0 until len) {
            lin[i] = kotlin.math.min(lin[i].toInt() * gain, Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun startRecordAndPlay() {
        record!!.startRecording()
        track!!.play()
        isRecording = true
    }

    private fun stopRecordAndPlay() {
        record!!.stop()
        track!!.pause()
        isRecording = false
    }
}