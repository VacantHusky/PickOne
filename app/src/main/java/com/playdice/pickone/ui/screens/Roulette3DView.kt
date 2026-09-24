package com.playdice.pickone.ui.screens

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.graphics.PixelFormat
import kotlin.math.cos
import kotlin.math.sin
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** A small real OpenGL ES model: a beveled wheel, six chambers and a rolling ball. */
class Roulette3DView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {
    private val sceneRenderer = RouletteRenderer()

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setRenderer(sceneRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        setZOrderOnTop(true)
    }

    fun setState(progress: Float, selectedIndex: Int, accent: Int, animating: Boolean, power: Float) {
        sceneRenderer.progress = progress
        sceneRenderer.selectedIndex = selectedIndex
        sceneRenderer.accent = accent
        sceneRenderer.animating = animating
        sceneRenderer.power = power
        requestRender()
    }
}

private class RouletteRenderer : GLSurfaceView.Renderer {
    var progress = 0f
    var selectedIndex = 0
    var accent = 0xff4f8d73.toInt()
    var animating = false
    var power = .5f

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var matrixHandle = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private var width = 1
    private var height = 1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        matrixHandle = GLES20.glGetUniformLocation(program, "uMvp")
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w
        height = h.coerceAtLeast(1)
        GLES20.glViewport(0, 0, w, height)
        Matrix.frustumM(projection, 0, -1f, 1f, -height.toFloat() / w.coerceAtLeast(1), height.toFloat() / w.coerceAtLeast(1), 2.2f, 8f)
        Matrix.setLookAtM(view, 0, 0f, 0f, 4.2f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, 24f, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, -8f, 0f, 1f, 0f)

        val wheelRotation = if (animating) progress * (720f + power * 1080f) else 0f
        Matrix.rotateM(model, 0, wheelRotation, 0f, 0f, 1f)
        drawCylinder(.92f, .18f, 48, 0x6e3f2818)
        drawDisk(.82f, .095f, 48, accent)
        repeat(6) { chamber ->
            val angle = Math.toRadians((-90 + chamber * 60).toDouble())
            val x = cos(angle).toFloat() * .53f
            val y = sin(angle).toFloat() * .53f
            val saved = model.copyOf()
            Matrix.translateM(model, 0, x, y, .105f)
            drawDisk(.145f, .012f, 24, 0xff131515.toInt())
            saved.copyInto(model)
        }
        val saved = model.copyOf()
        val targetAngle = -90f + (selectedIndex % 6) * 60f
        val ballAngle = if (animating) {
            targetAngle + (progress - 1f) * (1080f + power * 1800f) - (1f - progress) * 42f
        } else targetAngle
        val ballRadians = Math.toRadians(ballAngle.toDouble())
        Matrix.translateM(model, 0, cos(ballRadians).toFloat() * 1.0f, sin(ballRadians).toFloat() * 1.0f, .2f)
        drawSphere(.085f, 12, 8, 0xffdce1e7.toInt())
        saved.copyInto(model)
    }

    private fun drawDisk(radius: Float, depth: Float, segments: Int, color: Int) {
        drawCylinder(radius, depth, segments, color, topOnly = true)
    }

    private fun drawCylinder(radius: Float, depth: Float, segments: Int, color: Int, topOnly: Boolean = false) {
        val vertices = ArrayList<Float>()
        fun v(x: Float, y: Float, z: Float) { vertices += x; vertices += y; vertices += z }
        val zTop = depth
        for (i in 0 until segments) {
            val a = i * Math.PI * 2 / segments
            val b = (i + 1) * Math.PI * 2 / segments
            if (topOnly) {
                v(0f, 0f, zTop); v((cos(a) * radius).toFloat(), (sin(a) * radius).toFloat(), zTop); v((cos(b) * radius).toFloat(), (sin(b) * radius).toFloat(), zTop)
            } else {
                v((cos(a) * radius).toFloat(), (sin(a) * radius).toFloat(), zTop)
                v((cos(a) * radius).toFloat(), (sin(a) * radius).toFloat(), -depth)
                v((cos(b) * radius).toFloat(), (sin(b) * radius).toFloat(), -depth)
                v((cos(a) * radius).toFloat(), (sin(a) * radius).toFloat(), zTop)
                v((cos(b) * radius).toFloat(), (sin(b) * radius).toFloat(), -depth)
                v((cos(b) * radius).toFloat(), (sin(b) * radius).toFloat(), zTop)
            }
        }
        draw(vertices.toFloatArray(), color)
    }

    private fun drawSphere(radius: Float, rings: Int, segments: Int, color: Int) {
        val vertices = ArrayList<Float>()
        for (ring in 0 until rings) {
            val p1 = Math.PI * ring / rings - Math.PI / 2
            val p2 = Math.PI * (ring + 1) / rings - Math.PI / 2
            for (segment in 0 until segments) {
                val a1 = segment * Math.PI * 2 / segments
                val a2 = (segment + 1) * Math.PI * 2 / segments
                val points = listOf(
                    floatArrayOf(p1.toFloat(), a1.toFloat()),
                    floatArrayOf(p2.toFloat(), a1.toFloat()),
                    floatArrayOf(p2.toFloat(), a2.toFloat()),
                    floatArrayOf(p1.toFloat(), a1.toFloat()),
                    floatArrayOf(p2.toFloat(), a2.toFloat()),
                    floatArrayOf(p1.toFloat(), a2.toFloat()),
                )
                points.forEach { (p, a) ->
                    vertices += (cos(p) * cos(a) * radius.toDouble()).toFloat()
                    vertices += (sin(p) * radius.toDouble()).toFloat()
                    vertices += (cos(p) * sin(a) * radius.toDouble()).toFloat()
                }
            }
        }
        draw(vertices.toFloatArray(), color)
    }

    private fun draw(vertices: FloatArray, color: Int) {
        val buffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(vertices).position(0)
        Matrix.multiplyMM(mvp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, mvp, 0)
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvp, 0)
        GLES20.glUniform4f(colorHandle, ((color shr 16) and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f, 1f)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.size / 3)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun createProgram(vertex: String, fragment: String): Int {
        fun compile(type: Int, source: String): Int { val shader = GLES20.glCreateShader(type); GLES20.glShaderSource(shader, source); GLES20.glCompileShader(shader); return shader }
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, compile(GLES20.GL_VERTEX_SHADER, vertex)); GLES20.glAttachShader(p, compile(GLES20.GL_FRAGMENT_SHADER, fragment)); GLES20.glLinkProgram(p); return p
    }

    private companion object {
        const val VERTEX_SHADER = "attribute vec4 aPosition; uniform mat4 uMvp; void main(){ gl_Position=uMvp*aPosition; }"
        const val FRAGMENT_SHADER = "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }"
    }
}
