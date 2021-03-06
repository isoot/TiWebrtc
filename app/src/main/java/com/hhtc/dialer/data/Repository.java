package com.hhtc.dialer.data;

import android.arch.lifecycle.LiveData;

import com.hhtc.dialer.data.bean.CollectFavorite;
import com.hhtc.dialer.data.bean.DialerContact;
import com.hhtc.dialer.data.bean.RecentCallLog;
import com.hhtc.dialer.data.bean.UserInfo;
import com.hhtc.dialer.data.tradition.TraditionSynchronise;
import com.hhtc.dialer.main.recent.RecentModel;
import com.hhtc.dialer.thread.TelephoneThreadDispatcher;

import java.util.List;
import java.util.Objects;

public class Repository {

    private static Repository INSTANCE = null;

    private ToDoDatabase database;

    private Repository(ToDoDatabase database) {
        this.database = database;
    }

    public static Repository getInstance(ToDoDatabase database) {
        if (INSTANCE == null) {
            INSTANCE = new Repository(database);
        }
        return INSTANCE;
    }


    public void loadFavoriteAll(final LoadListLiveCallback<CollectFavorite> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            LiveData<List<CollectFavorite>> listLiveData = database.getCollectFavoriteDao().loadFavoriteAll();
            callback.onLiveData(listLiveData);
        }, TelephoneThreadDispatcher.DispatcherType.WORK);
    }


    public void insertFavorite(CollectFavorite favorite) {
        TelephoneThreadDispatcher.getInstance().execute(() -> database.getCollectFavoriteDao().insertFavorite(favorite), TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void updateFavorite(CollectFavorite collectFavorite) {
        TelephoneThreadDispatcher.getInstance().execute(() -> database.getCollectFavoriteDao().updateFavorite(collectFavorite), TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void deleteFavorite(CollectFavorite collectFavorite) {
        TelephoneThreadDispatcher.getInstance().execute(() -> database.getCollectFavoriteDao().deleteFavorite(collectFavorite), TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void loadContactLiveAll(final LoadLiveCallback<List<DialerContact>> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            if (callback != null) {
                LiveData<List<DialerContact>> dialerContactLiveData = database.getDialerContactDao().loadContactLiveAll();
                callback.onLiveData(dialerContactLiveData);
            }
        }, TelephoneThreadDispatcher.DispatcherType.WORK);

    }

    public void loadContactAll(final LoadDataCallback<DialerContact> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            if (callback != null) {
                List<DialerContact> dialerContacts = database.getDialerContactDao().loadContactAll();
                callback.onTasksLoaded(dialerContacts);
            }
        }, TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void insertContact(DialerContact contact) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            if (contact.isFavorite()) {
                CollectFavorite favorite = new CollectFavorite();
                favorite.setId(contact.getId());
                favorite.setName(contact.getName());
                favorite.setTel(contact.getTel());
                favorite.setVideo(contact.getVideo());
                favorite.setType(contact.getType());
                database.getCollectFavoriteDao().insertFavorite(favorite);
            } else {
                CollectFavorite favorite = database.getCollectFavoriteDao().loadFavoriteTradition(contact.getName(), contact.getTel(), contact.getType());
                if (Objects.nonNull(favorite)) {
                    database.getCollectFavoriteDao().deleteFavorite(favorite);
                }
            }
            database.getDialerContactDao().insertContact(contact);

        }, TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void updateContact(DialerContact contact) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            DialerContact contactByName = database.getDialerContactDao().loadContactByName(contact.getName());
            if (Objects.isNull(contactByName)) {
                database.getDialerContactDao().insertContact(contact);
            } else {
                contact.setId(contactByName.getId());
                database.getDialerContactDao().insertContact(contact);
            }

            CollectFavorite favorite = loadFavoriteById(contact.getId());

            if (Objects.isNull(favorite) && contact.isFavorite()) {
                favorite = new CollectFavorite();
                favorite.setId(contact.getId());
                favorite.setName(contact.getName());
                favorite.setTel(contact.getTel());
                favorite.setVideo(contact.getVideo());
                favorite.setType(contact.getType());
                database.getCollectFavoriteDao().insertFavorite(favorite);
            } else if (!Objects.isNull(favorite) && !contact.isFavorite()) {
                database.getCollectFavoriteDao().deleteFavorite(favorite);
            } else if (!Objects.isNull(favorite) && contact.isFavorite()) {
                favorite.setId(contact.getId());
                favorite.setName(contact.getName());
                favorite.setTel(contact.getTel());
                favorite.setVideo(contact.getVideo());
                favorite.setType(contact.getType());
                database.getCollectFavoriteDao().updateFavorite(favorite);
            }

            if (contact.getType() == RecentCallLog.TRADITIONAL) {
                //更新传统电话数据库
                TraditionSynchronise.changeContact(contact);
            }

        }, TelephoneThreadDispatcher.DispatcherType.WORK);

    }

    private CollectFavorite loadFavoriteById(long id) {
        return database.getCollectFavoriteDao().loadFavoriteById(id);
    }


    /**
     * 删除联系人
     *
     * @param contact
     */
    public void deleteContact(DialerContact contact) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            database.getDialerContactDao().deleteContact(contact);
            CollectFavorite favorite = loadFavoriteById(contact.getId());
            if (!Objects.isNull(favorite)) {
                deleteFavorite(favorite);
            }
            if (contact.getType() == RecentCallLog.TRADITIONAL) {
                TraditionSynchronise.deleteContact(contact);
            }
        }, TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void loadCallLogAll(final LoadListLiveCallback<RecentCallLog> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            LiveData<List<RecentCallLog>> listLiveData = database.getRecentCallLogDao().loadCallLogAll();
            callback.onLiveData(listLiveData);
        }, TelephoneThreadDispatcher.DispatcherType.WORK);
    }


    public void loadCallLogByName(String name, final LoadDataCallback<RecentCallLog> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            List<RecentCallLog> allCallLogByName = database.getRecentCallLogDao().loadCallLogByName(name);
            if (allCallLogByName.isEmpty()) {
                callback.onDataNotAvailable();
            } else {
                callback.onTasksLoaded(allCallLogByName);
            }
        }, TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void insertRecent(RecentCallLog callLog) {
        TelephoneThreadDispatcher.getInstance().execute(() -> database.getRecentCallLogDao().insertRecent(callLog), TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void updateRecent(RecentCallLog callLog) {
        TelephoneThreadDispatcher.getInstance().execute(() -> database.getRecentCallLogDao().updateRecent(callLog), TelephoneThreadDispatcher.DispatcherType.WORK);

    }


    public void deleteRecent(RecentCallLog callLog) {
        TelephoneThreadDispatcher.getInstance().execute(() -> database.getRecentCallLogDao().deleteRecent(callLog), TelephoneThreadDispatcher.DispatcherType.WORK);

    }

    public void loadContactLiveById(long contactId, LoadLiveCallback<DialerContact> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            LiveData<DialerContact> dialerContactLiveData = database.getDialerContactDao().loadContactById(contactId);
            callback.onLiveData(dialerContactLiveData);
        }, TelephoneThreadDispatcher.DispatcherType.WORK);
    }

    public void loadCallLogTradition(long time, int type, LoadUniqueCallback<RecentCallLog> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            RecentCallLog recentCallLog = database.getRecentCallLogDao().loadCallLogTradition(time, type);

            if (Objects.isNull(recentCallLog)) {
                callback.onDataNotAvailable();
            } else {
                callback.onTasksLoaded(recentCallLog);
            }
        }, TelephoneThreadDispatcher.DispatcherType.WORK);
    }

    public void loadFavoriteTradition(String name, String tel, int type, LoadUniqueCallback<CollectFavorite> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            CollectFavorite favorite = database.getCollectFavoriteDao().loadFavoriteTradition(name, tel, type);
            if (Objects.isNull(favorite)) {
                callback.onDataNotAvailable();
            } else {
                callback.onTasksLoaded(favorite);
            }
        }, TelephoneThreadDispatcher.DispatcherType.WORK);
    }

    public void loadContactLiveTradition(long contactId, LoadUniqueCallback<DialerContact> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            DialerContact dialerContact = database.getDialerContactDao().loadContactTradition(contactId);
            if (Objects.isNull(dialerContact)) {
                callback.onDataNotAvailable();
            } else {
                callback.onTasksLoaded(dialerContact);
            }
        }, TelephoneThreadDispatcher.DispatcherType.WORK);
    }

    public void loadUserInfo(LoadLiveCallback<UserInfo> callback) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            LiveData<UserInfo> userInfoLiveData = database.getUserInfoDao().loadUserInfo();
            callback.onLiveData(userInfoLiveData);
        }, TelephoneThreadDispatcher.DispatcherType.WORK);
    }

    public void insertUserInfo(UserInfo info) {
        TelephoneThreadDispatcher.getInstance().execute(() -> {
            database.getUserInfoDao().insertUserInfo(info);
        }, TelephoneThreadDispatcher.DispatcherType.WORK);
    }

    public UserInfo loadUserInfoWDF() {
        return database.getUserInfoDao().loadUserInfoWDF();
    }

    public ToDoDatabase getDatabase() {
        return database;
    }
}
