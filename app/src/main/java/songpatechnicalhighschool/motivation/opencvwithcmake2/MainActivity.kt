package songpatechnicalhighschool.motivation.opencvwithcmake2

import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import org.opencv.core.Core
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Semaphore


class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "FragmentActivity.TAG"
    private var matInput = Mat()
    private var matResult = Mat()

    //external fun ConvertRGBtoGray(matAddrInput: Long, matAddrResult: Long)
    external fun loadCascade(cascadeFileName: String): Long
    external fun detect(cascadeClassifier_face: Long, cascadeClassifier_eye: Long, matAddrInput: Long, matAddrResult: Long)
    var cascadeClassifier_face: Long = 0
    var cascadeClassifier_eye: Long = 0

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("opencv_java4")
        }
    }

    val loaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    activity_surface_view.enableView()
                    Log.d(TAG, "LoaderCallback Success")
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            } else {
                read_cascade_file()
            }
        } else {
            read_cascade_file()
        }

        activity_surface_view.visibility = SurfaceView.VISIBLE
        activity_surface_view.setCvCameraViewListener(this)
        activity_surface_view.setCameraIndex(1)
        loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        Log.d(TAG, "LoaderCallback")

        button.setOnClickListener {
            try {
                getWriteLock()

                val path = File("${Environment.getExternalStorageDirectory()} + /Images/")
                path.mkdirs()
                val file = File(path, "image.png")
                val filename = file.toString()

                Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGB, 4)
                val ret = Imgcodecs.imwrite(filename, matResult)
                if (ret) Log.d(TAG, "SUCESS")
                else Log.d(TAG, "FAIL")

                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(file)
                sendBroadcast(mediaScanIntent)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            releaseWriteLock()
        }
    }

    override fun onPause() {
        super.onPause()
        if (activity_surface_view != null) activity_surface_view.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "opResume : Internal OpenCV library not found.")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, loaderCallback)
        } else {
            Log.d(TAG, "opResume : OpenCV library found inside package. Using it!")
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity_surface_view != null) activity_surface_view.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {

        try {
            getWriteLock()

            matInput = inputFrame!!.rgba()

            if (matResult == null)
                matResult = Mat(matInput.rows(), matInput.cols(), matInput.type())

            //ConvertRGBtoGray(matInput.nativeObj, matResult.nativeObjAddr)
            Core.flip(matInput, matInput, 1)
            detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.nativeObjAddr, matResult.nativeObjAddr)
            Log.d(TAG, "detect")

        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        releaseWriteLock()

        return matResult
    }


    val PERMISSIONS_REQUEST_CODE = 1000
    val PERMISSIONS = arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")

    private fun hasPermissions(permission: Array<String>): Boolean {

        var result: Int

        for (perms in permission) {
            result = ContextCompat.checkSelfPermission(this, perms)
            if (result == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    //if (!cameraPermissionAccepted)
                    //    showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.")
                    val writePermissionAccepted = grantResults[1] === PackageManager.PERMISSION_GRANTED

                    if (!cameraPermissionAccepted || !writePermissionAccepted) {
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.")
                        return
                    } else {
                        read_cascade_file()
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun showDialogForPermission(msg: String) {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("알림")
        builder.setMessage(msg)
        builder.setCancelable(false)
        builder.setPositiveButton("예") { _, _ ->
            requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
        builder.setNegativeButton("아니오") { _, _ ->
            finish()
        }
        builder.create().show()
    }

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
            do{
                var read = inputStream.read(buffer)
                if(read == -1 )
                    break
                else{
                    outputStream.write(buffer, 0, read)
                }
            } while(true)

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

    val writeLock = Semaphore(1)

    fun getWriteLock(): InterruptedException {
        writeLock.acquire()
        return InterruptedException()
    }

    fun releaseWriteLock() {
        writeLock.release()
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
}
