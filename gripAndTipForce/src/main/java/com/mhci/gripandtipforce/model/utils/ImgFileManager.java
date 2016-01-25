package com.mhci.gripandtipforce.model.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.mhci.gripandtipforce.model.FileDirInfo;
import com.mhci.gripandtipforce.model.FileType;
import com.mhci.gripandtipforce.model.ProjectConfig;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ImgFileManager extends FileManager {
	public final static String DEBUG_TAG = ImgFileManager.class.toString();
	private Context mContext;
	private File imgDir = null;
	private FileType mFileType = null;
	
	public ImgFileManager(FileDirInfo dirInfo, Context context) {
		super(context, dirInfo.getFileType());
		//remain flexible for developer to do IO operation in another thread
		readUserConfig();
		
//		if(!FileDirInfo.isExternalStorageWritable()) {
//			Toast.makeText(context, "資料無法寫入指定資料夾,請再次確認設定無誤", Toast.LENGTH_LONG).show();
//		}
		
		mContext = context;
		mFileType = dirInfo.getFileType();
		initThreadAndHandler(mFileType.name() + System.currentTimeMillis());

		if(mFileType == FileType.TemplateImage) {
			templateImageFiles = new File[ProjectConfig.numOfGrades][];
		}

		imgDir = new File(dirInfo.getDirPath());
	}
	
	private HandlerThread mThread = null;
	private Handler mThreadHandler = null;
	
	private void initThreadAndHandler(String threadName) {
		mThread = new HandlerThread(threadName);
		mThread.start();
		mThreadHandler = new Handler(mThread.getLooper());
	}
	
	public void saveBMP(Bitmap bmp, String fileName, boolean bmpRecycle) {
		SaveBMPTask task = new SaveBMPTask(bmp, fileName, bmpRecycle);
		mThreadHandler.post(task);
	}
	
	private class SaveBMPTask implements Runnable {
		
		private Bitmap mBitmapToSave;
		private String fileNameForSaving;
		private boolean mBMPRecycle;
		public SaveBMPTask(Bitmap bmp, String fileName, boolean bmpRecycle) {
			mBitmapToSave = bmp;
			fileNameForSaving = fileName;
            mBMPRecycle = bmpRecycle;
		}
		
		@Override
		public void run() {
			FileOutputStream out = null;
			try {
				File file = null;
				if(imgDir != null) {
					file = new File(imgDir.getPath(), fileNameForSaving);
				}
				else {
					Toast.makeText(mContext, "Image Directory doesn't exist, failed to save image", Toast.LENGTH_LONG).show();
					return;
				}
				
				out = new FileOutputStream(file);
				mBitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
			    // PNG is a lossless format, the compression factor (100) is ignored
			} catch (Exception e) {
			    e.printStackTrace();
			} finally {
			    try {
			        if (out != null) {
			            out.close();
			        }
                    if(mBMPRecycle) {
                        mBitmapToSave.recycle();
                    }
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
			    
			}
		}
		
	}

	private File[][] templateImageFiles = null;

	//if index == the number of images for certain grade
	//then return "over"
	//if it successfully loads an image into imageView then return "success"
	//if error happens then return "error"
	//and this function would cache the list of images name
	public final static String errorMsg = "error";
	public final static String endMsg = "over";
	public final static String successMsg = "success";

	private ProgressDialog progressDialog = null;

	public String loadImageIntoImageViewWithSizeDependOnGradeAndIndex(
			int index,
			int grade,
			ImageView imageView) {

		if(templateImageFiles[grade] == null) {
			templateImageFiles[grade] = getTemplateImageFile(grade);
			if(templateImageFiles[grade] == null) {
				Toast.makeText(mContext, "模板資料夾不存在", Toast.LENGTH_LONG);
				return errorMsg;
			}
		}

		try {
			if (index >= templateImageFiles[grade].length) {
				return endMsg;
			}

            Log.d("loadImage","fileName:" + templateImageFiles[grade][index].getName());

			Picasso.with(mContext).
					load(templateImageFiles[grade][index]).
                    //resize(reqWidthInPixels, reqHeightInPixels)
					into(imageView, callbackAfterLoadingImage); //async function call

            try {
                progressDialog = null;
                progressDialog = ProgressDialog.show(mContext,
                        "忙碌中",
                        "圖片讀取中請稍候",
                        true,
                        false);
            }
            catch(Exception e) {
                progressDialog = null;
            }

		}
		catch(Exception e) {
			if(progressDialog != null) {
				progressDialog.dismiss();
			}
			Toast.makeText(mContext, e.getLocalizedMessage(), Toast.LENGTH_LONG);
			return errorMsg;
		}

		return successMsg;
	}

	private Callback callbackAfterLoadingImage = new Callback() {

		@Override
		public void onSuccess() {
			if(progressDialog != null) {
                try {
                    progressDialog.dismiss();
                }
                catch(Exception e) {}
			}
		}

		@Override
		public void onError() {
			if(progressDialog != null) {
                try {
                    progressDialog.dismiss();
                }
                catch(Exception e) {}
			}
			Toast.makeText(mContext, "圖片讀取失敗", Toast.LENGTH_LONG);
		}
	};

	public static File[] getTemplateImageFile(int grade) {
        File dir = new File(ProjectConfig.getTemplateImagesDirPath(grade));
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir.listFiles(ProjectConfig.templateImageNameFilter);
        }
        catch(Exception e) {
            return null;
        }
	}

//	private class LoadBMPTask implements Runnable {
//
//
//		private int calculateInSampleSize(
//				BitmapFactory.Options options, int reqWidth, int reqHeight) {
//			// Raw height and width of image
//			final int height = options.outHeight;
//			final int width = options.outWidth;
//			int inSampleSize = 1;
//
//			if (height > reqHeight || width > reqWidth) {
//
//				final int halfHeight = height / 2;
//				final int halfWidth = width / 2;
//
//				// Calculate the largest inSampleSize value that is a power of 2 and keeps both
//				// height and width larger than the requested height and width.
//				while ((halfHeight / inSampleSize) > reqHeight
//						&& (halfWidth / inSampleSize) > reqWidth) {
//					inSampleSize *= 2;
//				}
//			}
//
//			return inSampleSize;
//		}
//
//		private Bitmap decodeSampledBitmapFromResource(Resources res,
//													  int resId,
//													  int reqWidth,
//													  int reqHeight) {
//
//			// First decode with inJustDecodeBounds=true to check dimensions
//			final BitmapFactory.Options options = new BitmapFactory.Options();
//			options.inJustDecodeBounds = true;
//			BitmapFactory.decodeResource(res, resId, options);
//
//			// Calculate inSampleSize
//			options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//
//			// Decode bitmap with inSampleSize set
//			options.inJustDecodeBounds = false;
//			return BitmapFactory.decodeResource(res, resId, options);
//		}
//
//		@Override
//		public void run() {
//
//		}
//	}
	
}
