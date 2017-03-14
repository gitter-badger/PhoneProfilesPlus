package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

class ImageViewPreferenceDialog implements PreferenceManager.OnActivityDestroyListener {

    private ImageViewPreference imageViewPreference;
    MaterialDialog dialog;

    ImageViewPreferenceDialog(final Context context, ImageViewPreference preference, String imgSource,
                                     String imageIdentifier, boolean isImageResourceID)
    {
        this.imageViewPreference = preference;

        MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(context)
                .title(R.string.title_activity_image_view_preference_dialog)
                //.disableDefaultFonts()
                .autoDismiss(false)
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        MaterialDialogsPrefUtil.unregisterOnActivityDestroyListener(imageViewPreference, ImageViewPreferenceDialog.this);
                    }
                })
                .customView(R.layout.activity_imageview_resource_pref_dialog, false);

        if (imgSource.equals("resource_file"))
        {
            dialogBuilder.positiveText(R.string.imageview_resource_file_pref_dialog_gallery_btn);
            dialogBuilder.onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                    // zavolat galeriu na vyzdvihnutie image
                    if (Permissions.grantWallpaperPermissions(context, imageViewPreference))
                        imageViewPreference.startGallery();
                    dialog.dismiss();
                }
            });
        }

        MaterialDialogsPrefUtil.registerOnActivityDestroyListener(imageViewPreference, this);

        dialog = dialogBuilder.build();

        //noinspection ConstantConditions
        GridView gridView = (GridView)dialog.getCustomView().findViewById(R.id.imageview_resource_pref_dlg_gridview);
        gridView.setAdapter(new ImageViewPreferenceAdapter(context, imageIdentifier, isImageResourceID));

        gridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                imageViewPreference.setImageIdentifierAndType(Profile.profileIconId[position], true);
                dialog.dismiss();
            }

        });

    }

    public void show() {
        dialog.show();
    }

    @Override
    public void onActivityDestroy() {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }

}
