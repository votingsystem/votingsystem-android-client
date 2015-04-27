package org.votingsystem.android.util;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import org.votingsystem.android.contentprovider.UserContentProvider;
import org.votingsystem.dto.UserVSDto;
import org.votingsystem.util.ObjectUtils;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DBUtils {

    public static final String TAG = DBUtils.class.getSimpleName();

    public static UserVSDto extractInfoFromContactPickerIntent(final Intent intent, Context mContext) {
        Cursor cursor = null;
        try {
            Uri contactURI = intent.getData();
            String id = contactURI.getLastPathSegment();// Get the contact id from the Uri
            String phone = null;
            String email = null;
            String name = null;
            UserVSDto userVS = null;
            cursor = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
            if(cursor != null) {
                cursor.moveToFirst();
                if(cursor.getCount() > 0) {
                    phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                }
            }
            cursor = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + id, null, null);
            if(cursor != null) {
                cursor.moveToFirst();
                if(cursor.getCount() > 0)  email = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
            String[] args = { contactURI.toString() };
            cursor = mContext.getContentResolver().query(UserContentProvider.CONTENT_URI, null,
                    UserContentProvider.CONTACT_URI_COL + " = ?" , args, null);
            if(cursor != null) {
                cursor.moveToFirst();
                if(cursor.getCount() > 0) {
                    userVS = (UserVSDto) ObjectUtils.deSerializeObject(cursor.getBlob(
                        cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
                }
            }
            if(userVS == null) {
                userVS = new UserVSDto();
                userVS.setName(name);
                userVS.setPhone(phone);
                userVS.setEmail(email);
            }
            userVS.setContactURI(contactURI);
            LOGD(TAG, "email: " + email + " - phone: " + phone + " - displayName: " + name);
            return userVS;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }
}
