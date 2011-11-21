package example.filedownload;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

public class ImageActivity  extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        	WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        setContentView(R.layout.main);
        
	//��ȡBundle������
        Intent intent=this.getIntent();
	Bundle bl= intent.getExtras();
	int pos =bl.getInt("url");

	ImageView view = (ImageView) findViewById(R.id.imageView);
        
	Bitmap bitmap = Utils.getLoacalBitmap(Utils.APK_ROOT + Utils.getFileNameFromUrl(Utils.url[pos]));
	view.setImageBitmap(bitmap);

        view.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
//		Intent intent = new Intent(ImageActivity.this, FileDownloadActivity.class);
//		startActivity(intent);
	        finish();
	    }
            
        });
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (resultCode)
		{
			//�������
		case RESULT_OK:
			//��ȡBundle������
			Bundle bl= data.getExtras();
			int pos =bl.getInt("url");

			ImageView view = (ImageView) findViewById(R.id.imageView);
		        
			Bitmap bitmap = Utils.getLoacalBitmap(Utils.APK_ROOT + Utils.getFileNameFromUrl(Utils.url[pos]));
			view.setImageBitmap(bitmap);
			break;
		default:
			break;
		}
	}
}