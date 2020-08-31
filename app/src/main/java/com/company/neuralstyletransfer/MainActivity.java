package com.company.neuralstyletransfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {
    private final String IP_ADDRESS = "192.168.1.6";
    private final int PORT_NUMBER = 8080;
    private final int PICK_IMAGE_CONTENT = 1444;
    private final int PICK_IMAGE_STYLE = 1222;
    private final int READ_STORAGE = 1999;
    private final int CROP_PICTURE = 1888;
    private final int HEADER = 64;
    private final String ENCODE_FORMAT = "utf-8";
    private final String STYLE_FORMAT = "$STYLE";
    private final String CONTENT_FORMAT = "$CONTENT";
    private final String RESULT_FORMAT = "$RES";
    private final String DISCONNECT = "!DISCONNECT";
    private int CURR_SELECTOR;
    private Bitmap contentBitmap, styleBitmap;
    private Socket socket;
    private PrintWriter output;
    private DataOutputStream dos;
    private BufferedReader input;
    private Uri contentUri, styleUri;
    private ImageView contentImageView, styleImageView;
    private Button proceedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentImageView = findViewById(R.id.main_image_content);
        styleImageView = findViewById(R.id.main_image_style);
        proceedButton = findViewById(R.id.main_proceed_button);

        CURR_SELECTOR = PICK_IMAGE_CONTENT;
        new Thread(new ClientThread()).start();
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    System.out.println("Connecting...");
//                    socket = new Socket(ipAddress, portNumber);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(), String.valueOf(socket.isConnected()), Toast.LENGTH_LONG).show();
//                        }
//                    });
//                    System.out.println(socket.isConnected());
//                }catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        thread.start();
//        connectToServer();
        contentImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CURR_SELECTOR = PICK_IMAGE_CONTENT;
                if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    imageSelector(PICK_IMAGE_CONTENT);
                } else {
                    requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_STORAGE);
                }
            }
        });

        styleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CURR_SELECTOR = PICK_IMAGE_STYLE;
                if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    imageSelector(PICK_IMAGE_STYLE);
                } else {
                    requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_STORAGE);
                }
            }
        });

        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendImage(contentBitmap, CONTENT_FORMAT);
//                new Thread(new MessageSender("Hello!")).start();
            }
        });
    }

    private void imageSelector(int requestCode) {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
        startActivityForResult(chooserIntent, requestCode);
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String permission, int requestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permission},
                requestCode);
    }

    private void connectToServer() {
        try {
            socket = new Socket(IP_ADDRESS, PORT_NUMBER);
            Toast.makeText(getApplicationContext(), String.valueOf(socket.isConnected()), Toast.LENGTH_LONG).show();
            System.out.println(socket.isConnected());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendImage(Bitmap bitmap, String imgType) {
        if (bitmap != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int quality = (bitmap.getByteCount() > 200000) ? 40 : 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
            byte[] b = bos.toByteArray();
            String encodedImage = Base64.encodeToString(b, Base64.NO_WRAP);
            encodedImage = imgType + encodedImage;
            System.out.println(encodedImage.length());
//            decodeImage(encodedImage, imgType);
            new Thread(new MessageSender(encodedImage)).start();
        }
    }

    private String utfEncodeString(String s) {
        String res = null;
        try {
            res = URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private void sendMessage(final String message) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Connecting...");
                    socket = new Socket(IP_ADDRESS, PORT_NUMBER);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), String.valueOf(socket.isConnected()), Toast.LENGTH_LONG).show();
                        }
                    });
                    try {
                        String encodedLength = URLEncoder.encode(String.valueOf(message.length()), ENCODE_FORMAT);
                        System.out.println(encodedLength + " " + encodedLength.length());
                        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                        StringBuilder msgLength = new StringBuilder();
                        msgLength.append(message.length());
                        int currLen = msgLength.length();
                        for (int i = 0; i < HEADER - currLen; i++) {
                            msgLength.append(" ");
                        }
                        System.out.println(msgLength.toString() + " " + msgLength.length());
//                        dos.writeUTF(msgLength.toString());
//                        dos.writeChars(msgLength.toString());
                        dos.write(msgLength.toString().getBytes());
                        dos.write(message.getBytes());
//                        PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
//                        pw.write(msgLength.toString());
//                        pw.write(message);
//                        pw.close();

                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        String recvMessage = dis.readUTF();
                        System.out.println("Message received: " + recvMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println(socket.isConnected());
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
//        try {
//            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//            StringBuilder msgLength = new StringBuilder();
//            msgLength.append(message.length());
//            for (int i = 0; i < HEADER - msgLength.length(); i++) {
//                msgLength.append(" ");
//            }
//            dos.writeUTF(msgLength.toString());
//
//            DataInputStream dis = new DataInputStream(socket.getInputStream());
//            String recvMessage = dis.readUTF();
//            System.out.println(recvMessage);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void decodeImage(String encodedImage, String imgType) {
        try {
            encodedImage = URLEncoder.encode(encodedImage, "UTF-8");
            System.out.println(encodedImage.charAt(0));
            encodedImage = URLDecoder.decode(encodedImage, "UTF-8");
            encodedImage = encodedImage.substring(imgType.length());
            System.out.println(encodedImage.charAt(0));
            byte[] b = Base64.decode(encodedImage.getBytes(), Base64.NO_WRAP);
            styleBitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            styleImageView.setImageBitmap(styleBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case READ_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permission granted");
                    imageSelector(CURR_SELECTOR);
                } else {
                    Toast.makeText(getApplicationContext(), "Storage permission is required", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_CONTENT && data != null) {
            Uri uri = data.getData();
            contentUri = data.getData();
            CropImage.activity(contentUri).start(this);
            System.out.println("Content Image Picked");
        }
        if (requestCode == PICK_IMAGE_STYLE && data != null) {
            Uri uri = data.getData();
            styleUri = data.getData();
            CropImage.activity(styleUri).start(this);
            System.out.println("Style Image picked");
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    if (CURR_SELECTOR == PICK_IMAGE_CONTENT) {
                        contentImageView.setImageBitmap(bitmap);
                        contentBitmap = bitmap;
                    }
                    if (CURR_SELECTOR == PICK_IMAGE_STYLE) {
                        styleImageView.setImageBitmap(bitmap);
                        styleBitmap = bitmap;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                System.out.println(result.getError().getMessage());
            }
        }
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {
            try {
                InetAddress serverAddress = InetAddress.getByName(IP_ADDRESS);
                socket = new Socket(serverAddress, PORT_NUMBER);
                dos = new DataOutputStream(socket.getOutputStream());
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                new Thread(new MessageReceiver()).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class MessageReceiver implements Runnable {
        @Override
        public void run() {
            System.out.println("Starting Message Receiver...");
            while(true) {
                try {
                    System.out.println("Message Received");
                    String message = input.readLine();
                    System.out.println("Message Received....");
                    System.out.println(message);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    class MessageSender implements Runnable {
        private String message;

        MessageSender(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {
                StringBuilder msgLength = new StringBuilder();
                msgLength.append(message.length());
                int currLen = msgLength.length();
                for (int i = 0; i < HEADER - currLen; i++) {
                    msgLength.append(" ");
                }
//            System.out.println(msgLength.toString() + " " + msgLength.length());
//            output.write(msgLength.toString());
//            output.flush();
//            output.write(message);
//            output.flush();
                System.out.println(message);
                dos.write(msgLength.toString().getBytes());
                dos.flush();
                dos.write(message.getBytes());
                dos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}