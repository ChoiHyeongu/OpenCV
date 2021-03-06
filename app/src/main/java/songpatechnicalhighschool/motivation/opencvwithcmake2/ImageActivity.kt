package songpatechnicalhighschool.motivation.opencvwithcmake2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_image.*
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Semaphore

class ImageActivity : AppCompatActivity() {

    lateinit var img: Bitmap
    lateinit var uri: Uri
    private var matInput = Mat()
    private var matResult = Mat()
    var cascadeClassifier_face: Long = 0
    var cascadeClassifier_eye: Long = 0

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("opencv_java4")
        }
    }


    external fun detect(cascadeClassifier_face: Long, cascadeClassifier_eye: Long, matAddrInput: Long, matAddrResult: Long): IntArray
    external fun loadCascade(cascadeFileName: String): Long

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        read_cascade_file()

        selectImageButton.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, 1)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                try {
                    uri = data!!.data
                    Toast.makeText(this, uri.toString(), Toast.LENGTH_LONG).show()
                    val input: InputStream = contentResolver.openInputStream(data!!.data)
                    img = BitmapFactory.decodeStream(input)
                    imageView.setImageBitmap(img)
                    input.close()
                    faceDetect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun faceDetect(): Mat {

        val option = BitmapFactory.Options()
        option.inMutable = true
        val myBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri), null, option)
        val tempBitmap = myBitmap.copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(tempBitmap, matInput)

        try {
            getWriteLock()

            if (matResult == null)
                matResult = Mat(matInput.rows(), matInput.cols(), matInput.type())

            //ConvertRGBtoGray(matInput.nativeObj, matResult.nativeObjAddr)
            Core.flip(matInput, matInput, 1)
            Log.d("Image Detect", "Before detect")
            val faces = detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.nativeObjAddr, matResult.nativeObjAddr)
            Log.d("Image Detect", "Return : ${faces[0]}, ${faces[1]}, ${faces[2]}, ${faces[3]}")
            Log.d("Image Detect", "detect")

            val pnt = Paint()
            pnt.color = Color.GREEN
            pnt.strokeWidth = 5F
            pnt.style = Paint.Style.STROKE

            val myRectPaint = Paint()
            myRectPaint.strokeWidth = 3F
            myRectPaint.color = Color.RED
            myRectPaint.style = Paint.Style.STROKE

            val tempBitmap = Bitmap.createBitmap(img.width, img.height, Bitmap.Config.RGB_565)
            val tempCanvas = Canvas(tempBitmap)
            tempCanvas.drawBitmap(img, 0F, 0F, null)

            val x1 = faces[0]
            val y1 = faces[1]
            val x2 = x1+faces[2]
            val y2 = y1+faces[3]

            tempCanvas.drawCircle(x2.toFloat(), y2.toFloat(), 10F, myRectPaint)

            imageView.setImageDrawable(BitmapDrawable(resources, tempBitmap))
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        releaseWriteLock()

        return matResult
    }

    fun getWriteLock(): InterruptedException {
        writeLock.acquire()
        return InterruptedException()
    }

    fun releaseWriteLock() {
        writeLock.release()
    }

    val writeLock = Semaphore(1)

    private fun copyFile(filename: String) {
        val baseDir = Environment.getExternalStorageDirectory().path
        val pathDir = baseDir + File.separator + filename

        val assetManager = this.assets

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            Log.d("FragmentActivity.TAG", "copyFile :: 다음 경로로 파일복사 $pathDir")
            inputStream = assetManager.open(filename)
            outputStream = FileOutputStream(pathDir)

            val buffer = ByteArray(1024)
            do {
                var read = inputStream.read(buffer)
                if (read == -1)
                    break
                else {
                    outputStream.write(buffer, 0, read)
                }
            } while (true)

            inputStream.close()
            inputStream = null
            outputStream.flush()
            outputStream.close()
            outputStream = null
        } catch (e: Exception) {
            Log.d("FragmentActivity.TAG", "copyFile :: 파일 복사 중 예외 발생 $e")
        }
    }

    private fun read_cascade_file() {
        copyFile("haarcascade_frontalface_alt.xml")
        copyFile("haarcascade_eye_tree_eyeglasses.xml")

        cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml")
        Log.d("FragmentActivity.TAG", "read_cascade_file:")
        cascadeClassifier_eye = loadCascade("haarcascade_eye_tree_eyeglasses.xml")
        Log.d("FragmentActivity.TAG", "read_cascade_file:")
    }
}


