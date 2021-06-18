package com.kms.camera


import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.net.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    companion object{
        var socket = Socket()
        var server = ServerSocket()
        lateinit var writeSocket: DataOutputStream
        lateinit var readSocket: DataInputStream
        lateinit var cManager : ConnectivityManager

        var ip = "54.166.69.100"
        var port = 23023
        var mHandler = Handler()
        var closed = false
    }


    val CAMEARA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val STORAGE_PERMISSION = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val FLAG_PERM_CAMERA = 98
    val FLAG_PERM_STORAGE = 99

    val FLAG_REQ_CAMERA = 101
    val FLAG_REQ_GALLERY = 102


    fun isPermitted(permissions:Array<String>) : Boolean {
        for(permission in permissions) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    //카메라 누른부분
    fun openCamera(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, FLAG_REQ_CAMERA)
    }

    fun openGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        startActivityForResult(intent,FLAG_REQ_GALLERY)
    }


    fun saveImageFile(filename:String,mimeType:String,bitmap:Bitmap):Uri?{
        Log.d("갤러리","tttt")
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME,filename)
        values.put(MediaStore.Images.Media.MIME_TYPE,mimeType)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            values.put(MediaStore.Images.Media.IS_PENDING,1)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
        try {
            if (uri != null) {
                var descriptor = contentResolver.openFileDescriptor(uri, "w")
                if (descriptor != null) {
                    val fos = FileOutputStream(descriptor.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.close()
                    return uri
                }
            }
        }catch(e:Exception){
            Log.e("Camera","${e.localizedMessage}")
        }
        return null
    }
    fun newFileName() : String{
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())
        return filename
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("카메라","req = $requestCode,result = $resultCode,data = $data")
        //cManager = applicationContext.getSystemService(Context.CONNECTIVITY_DIAGNOSTICS_SERVICE) as ConnectivityManager
        //server.close()
        //socket.close()

        if(resultCode== Activity.RESULT_OK) {
            when (requestCode) {
                //카메라라면
                FLAG_REQ_CAMERA -> {
                    if(data?.extras?.get("data")!=null) {
                        val bitmap = data?.extras?.get("data") as Bitmap

                        var filename = newFileName()
                        val uri = saveImageFile(filename,"image/jpg",bitmap)
                        //파일 생성 후 갤러리에 저장.
                        imagePreview.setImageURI(uri)
                    }
                }
                FLAG_REQ_GALLERY->{
                    val uri = data?.data
                    //이미지를 보여줌.
                    imagePreview.setImageURI(uri)
                }
            }
        }
    }
    //허가를 받는 부분.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            FLAG_PERM_CAMERA ->{
                var checked = true
                for(grant in grantResults){
                    if(grant != PackageManager.PERMISSION_GRANTED){
                        checked = false
                        break
                    }
                }
                if(checked){
                    openCamera()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //카메라 버튼 누를 시
        buttonCamera.setOnClickListener {
            //허가가 이미 받아져 있으면
            if(isPermitted(CAMEARA_PERMISSION)) {
                //카메라를 킴
                openCamera()
            }else{
                //허가되지 않았으면 허가를 받는다.
                ActivityCompat.requestPermissions(this,CAMEARA_PERMISSION, FLAG_PERM_CAMERA)
            }
        }
        //갤러리 버튼을 누를 시
        buttonGallery.setOnClickListener {
            //허가가 이미 받아져 있으면
            if(isPermitted(STORAGE_PERMISSION)) {
                //갤러리를 킨다.
                openGallery()
            }else{
                //허가되지 않았으면 허가를 받는다.
                ActivityCompat.requestPermissions(this,STORAGE_PERMISSION, FLAG_PERM_STORAGE)
            }
        }



        restorebutton.setOnClickListener{
            Connect().start()
            Log.d("서버","asdasdasd")

            try {
                //writeSocket.writeInt(2)
                Log.d("서버","보내기 전")
                writeSocket.writeChars("hi")
                Log.d("서버","보내기 완료")
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
                mHandler.obtainMessage(12).apply { sendToTarget() }
                Log.d("서버","보내기실패")

            }


         //   if(isPermitted(STORAGE_PERMISSION)) {
         //       //갤러리를 킨다.
          //      var intent = Intent(Intent.ACTION_PICK)
          //      intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
           //     intent.type = "image/*"
            //    startActivityForResult(intent,FLAG_REQ_GALLERY)
            //}else{
                //허가되지 않았으면 허가를 받는다.
         //      ActivityCompat.requestPermissions(this,STORAGE_PERMISSION, FLAG_PERM_STORAGE)
           // }


        }

        mHandler = object : Handler(Looper.getMainLooper()){  //Thread들로부터 Handler를 통해 메시지를 수신
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when(msg.what){
                    1-> Toast.makeText(this@MainActivity, "IP 주소가 잘못되었거나 서버의 포트가 개방되지 않았습니다.", Toast.LENGTH_SHORT).show()
                    2->Toast.makeText(this@MainActivity, "서버 포트 "+port +"가 준비되었습니다.", Toast.LENGTH_SHORT).show()
                    3->Toast.makeText(this@MainActivity, msg.obj.toString(), Toast.LENGTH_SHORT).show()
                    4->Toast.makeText(this@MainActivity, "연결이 종료되었습니다.", Toast.LENGTH_SHORT).show()
                    5->Toast.makeText(this@MainActivity, "이미 사용중인 포트입니다.", Toast.LENGTH_SHORT).show()
                    6->Toast.makeText(this@MainActivity, "서버 준비에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    7->Toast.makeText(this@MainActivity, "서버가 종료되었습니다.", Toast.LENGTH_SHORT).show()
                    8->Toast.makeText(this@MainActivity, "서버가 정상적으로 닫히는데 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    11->Toast.makeText(this@MainActivity, "서버에 접속하였습니다.", Toast.LENGTH_SHORT).show()
                    //12->Toast.makeText(this@MainActivity, "메시지 전송에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    13->Toast.makeText(this@MainActivity, "클라이언트와 연결되었습니다.",Toast.LENGTH_SHORT).show()
                    14->Toast.makeText(this@MainActivity,"서버에서 응답이 없습니다.", Toast.LENGTH_SHORT).show()
                    15->Toast.makeText(this@MainActivity, "서버와의 연결을 종료합니다.", Toast.LENGTH_SHORT).show()
                    16->Toast.makeText(this@MainActivity, "클라이언트와의 연결을 종료합니다.", Toast.LENGTH_SHORT).show()
                    17->Toast.makeText(this@MainActivity, "포트가 이미 닫혀있습니다.", Toast.LENGTH_SHORT).show()
                    18->Toast.makeText(this@MainActivity, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }






    }


    class Connect:Thread(){

        override fun run() = try{
            Log.d("서버","$ip,$port")
            socket = Socket(ip, port)
            writeSocket = DataOutputStream(socket.getOutputStream())
            readSocket = DataInputStream(socket.getInputStream())
            writeSocket.writeChars("zzzzzzz")
            val b = readSocket.read()
            Log.d("서버","b전  $b")

            if(b==83){    //서버로부터 접속이 확인되었을 때
                mHandler.obtainMessage(11).apply {
                    sendToTarget()
                }
                Log.d("서버","서버접속")

                val mThread = SendMessage()
                mThread.start()
            }else{    //서버 접속에 성공하였으나 서버가 응답을 하지 않았을 때
                mHandler.obtainMessage(14).apply {
                    sendToTarget()
                }
                Log.d("서버","서버 응답 x")
                socket.close()
            }


        }catch(e:Exception){    //연결 실패
            val state = 1
            mHandler.obtainMessage(state).apply {
                sendToTarget()
            }
            Log.d("서버","연결실패")
            socket.close()
        }

    }

    class ClientSocket:Thread(){
        override fun run() {
            try{
                while (true) {
                    val ac = readSocket.read()
                    if(ac == 2) {    //서버로부터 메시지 수신 명령을 받았을 때
                        val bac = readSocket.readUTF()
                        val input = bac.toString()
                        val recvInput = input.trim()

                        val msg = mHandler.obtainMessage()
                        msg.what = 3
                        msg.obj = recvInput
                        mHandler.sendMessage(msg)
                    }else if(ac == 10){    //서버로부터 접속 종료 명령을 받았을 때
                        mHandler.obtainMessage(18).apply {
                            sendToTarget()
                        }
                        socket.close()
                        break
                    }
                }
            }catch(e: SocketException){    //소켓이 닫혔을 때
                mHandler.obtainMessage(15).apply {
                    sendToTarget()
                }
            }
        }
    }

    class Disconnect:Thread(){
        override fun run() {
            try{
                writeSocket.write(10)    //서버에게 접속 종료 명령 전송
                socket.close()
            }catch(e:Exception){

            }
        }
    }

    class SetServer:Thread(){

        override fun run(){
            try{
                server = ServerSocket(port)    //포트 개방
                mHandler.obtainMessage(2, "").apply {
                    sendToTarget()
                }

                while(true) {
                    socket = server.accept()
                    writeSocket = DataOutputStream(socket.getOutputStream())
                    readSocket = DataInputStream(socket.getInputStream())

                    writeSocket.write(1)    //클라이언트에게 서버의 소켓 생성을 알림
                    mHandler.obtainMessage(13).apply {
                        sendToTarget()
                    }
                    while (true) {
                        val ac = readSocket.read()
                        if(ac==10){    //클라이언트로부터 소켓 종료 명령 수신
                            mHandler.obtainMessage(16).apply {
                                sendToTarget()
                            }
                            break
                        }else if(ac == 2){    //클라이언트로부터 메시지 전송 명령 수신
                            val bac = readSocket.readUTF()
                            val input = bac.toString()
                            val recvInput = input.trim()

                            val msg = mHandler.obtainMessage()
                            msg.what = 3
                            msg.obj = recvInput
                            mHandler.sendMessage(msg)    //핸들러에게 클라이언트로 전달받은 메시지 전송
                        }
                    }
                }

            }catch(e: BindException) {    //이미 개방된 포트를 개방하려 시도하였을때
                mHandler.obtainMessage(5).apply {
                    sendToTarget()
                }
            }catch(e:SocketException){    //소켓이 닫혔을 때
                mHandler.obtainMessage(7).apply {
                    sendToTarget()
                }
            }
            catch(e:Exception){
                if(!closed) {
                    mHandler.obtainMessage(6).apply {
                        sendToTarget()
                    }
                }else{
                    closed = false
                }
            }
        }
    }

    class CloseServer:Thread(){
        override fun run(){
            try{
                closed = true
                writeSocket.write(10)    //클라이언트에게 서버가 종료되었음을 알림
                socket.close()
                server.close()
            }catch(e:Exception){
                e.printStackTrace()
                mHandler.obtainMessage(8).apply {
                    sendToTarget()
                }
            }
        }
    }

    class SendMessage:Thread(){
        private lateinit var msg:String

        override fun run() {
            try{
                writeSocket.writeChars("intote")   //메시지 내용
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = MediaStore.Images.Media.CONTENT_TYPE
                writeSocket.writeUTF(intent.type)


            }catch(e:Exception){
                e.printStackTrace()
                mHandler.obtainMessage(12).apply {
                    sendToTarget()
                }
            }
        }
    }

    class ShowInfo:Thread(){

        override fun run(){
            lateinit var ip:String
            var breakLoop = false
            val en = NetworkInterface.getNetworkInterfaces()
            while(en.hasMoreElements()){
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while(enumIpAddr.hasMoreElements()){
                    val inetAddress = enumIpAddr.nextElement()
                    if(!inetAddress.isLoopbackAddress && inetAddress is Inet4Address){
                        ip = inetAddress.hostAddress.toString()
                        breakLoop = true
                        break
                    }
                }
                if(breakLoop){
                    break
                }
            }

            val msg = mHandler.obtainMessage()
            msg.what = 9
            msg.obj = ip
            mHandler.sendMessage(msg)
        }
    }


}