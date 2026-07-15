package com.aistudio.sepatify.player

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

@UnstableApi
class FftAudioProcessor(
    private val onFftReady: (FloatArray) -> Unit
) : AudioProcessor {

    private var inputAudioFormat = AudioFormat.NOT_SET
    private var isActive = false
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    // We process blocks of 1024 samples (typical for audio analysis)
    private val fftSize = 1024
    private val pcmBuffer = FloatArray(fftSize)
    private var pcmBufferIndex = 0

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        // We only support PCM 16-bit encoding for visualizer processing
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        isActive = true
        return inputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val remainingBytes = inputBuffer.remaining()

        // Prepare local output buffer to pass data down the audio pipeline
        if (buffer.capacity() < remainingBytes) {
            buffer = ByteBuffer.allocateDirect(remainingBytes).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        // Process 16-bit PCM stereo/mono samples
        val channelCount = inputAudioFormat.channelCount
        while (inputBuffer.hasRemaining() && pcmBufferIndex < fftSize) {
            // Read 16-bit PCM sample (2 bytes)
            val sample = inputBuffer.short.toFloat() / Short.MAX_VALUE

            // If stereo, average the channels or just take the left channel
            if (channelCount > 1 && inputBuffer.hasRemaining()) {
                val rightSample = inputBuffer.short.toFloat() / Short.MAX_VALUE
                pcmBuffer[pcmBufferIndex] = (sample + rightSample) / 2f
            } else {
                pcmBuffer[pcmBufferIndex] = sample
            }

            // Put the original bytes back to the pipeline so sound continues playing
            buffer.putShort((sample * Short.MAX_VALUE).toInt().toShort())
            if (channelCount > 1) {
                buffer.putShort((sample * Short.MAX_VALUE).toInt().toShort())
            }

            pcmBufferIndex++

            if (pcmBufferIndex >= fftSize) {
                // Buffer is full, run FFT calculation
                calculateFft(pcmBuffer)
                pcmBufferIndex = 0
            }
        }

        buffer.flip()
        outputBuffer = buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
        outputBuffer = AudioProcessor.EMPTY_BUFFER
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer == AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        pcmBufferIndex = 0
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        isActive = false
    }

    // A simple, fast In-place Radix-2 Decimation-in-Time FFT Algorithm
    private fun calculateFft(input: FloatArray) {
        val n = input.size
        val real = input.copyOf()
        val imag = FloatArray(n) { 0f }

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal
            }
            var m = n shr 1
            while (m >= 2 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Cooley-Tukey FFT
        var size = 2
        while (size <= n) {
            val halfSize = size shr 1
            val tabstep = n / size
            for (i in 0 until n step size) {
                for (k in 0 until halfSize) {
                    val angle = -2 * Math.PI * k / size
                    val wr = Math.cos(angle).toFloat()
                    val wi = Math.sin(angle).toFloat()

                    val pr = real[i + k + halfSize] * wr - imag[i + k + halfSize] * wi
                    val pi = real[i + k + halfSize] * wi + imag[i + k + halfSize] * wr

                    real[i + k + halfSize] = real[i + k] - pr
                    imag[i + k + halfSize] = imag[i + k] - pi
                    real[i + k] += pr
                    imag[i + k] += pi
                }
            }
            size = size shl 1
        }

        // Pass calculated spectrum up to UI
        processFftSpectrum(real, imag)
    }

    private fun processFftSpectrum(real: FloatArray, imag: FloatArray) {
        val n = real.size / 2 // We only need the first half (positive frequencies)
        val magnitudes = FloatArray(n)
        for (i in 0 until n) {
            magnitudes[i] = Math.hypot(real[i].toDouble(), imag[i].toDouble()).toFloat()
        }

        // Pass the magnitudes array to the callback
        onFftReady(magnitudes)
    }
}