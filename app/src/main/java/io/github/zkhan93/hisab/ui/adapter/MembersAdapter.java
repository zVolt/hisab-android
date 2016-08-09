package io.github.zkhan93.hisab.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import io.github.zkhan93.hisab.R;
import io.github.zkhan93.hisab.model.User;
import io.github.zkhan93.hisab.model.callback.UserItemActionClickClbk;
import io.github.zkhan93.hisab.model.ui.ExUser;
import io.github.zkhan93.hisab.model.viewholder.EmptyVH;
import io.github.zkhan93.hisab.model.viewholder.UserVH;

/**
 * Created by Zeeshan Khan on 8/9/2016.
 */
public class MembersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        ChildEventListener {

    public static String TAG = MembersAdapter.class.getSimpleName();

    private List<ExUser> members;
    private UserItemActionClickClbk actionCallback;
    private DatabaseReference dbRef, shareRef;
    private ValueEventListener moderatorListener;
    private User me;

    {
        moderatorListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                members.add(new ExUser(user));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "cancelled");

            }
        };
    }

    public MembersAdapter(UserItemActionClickClbk actionCallback, User me, String groupId) {
        members = new ArrayList<>();
        this.actionCallback = actionCallback;
        dbRef = FirebaseDatabase.getInstance().getReference();
        shareRef = dbRef.child("shareWith").child(groupId);
        dbRef.child("groups").child(me.getId()).child(groupId).child("moderator")
                .addListenerForSingleValueEvent(moderatorListener);
        this.me = me;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE.NORMAL:
                holder = new UserVH(inflater.inflate(R.layout.member_item,
                        parent, false), actionCallback);
                break;
            case VIEW_TYPE.EMPTY:
            default:
                holder = new EmptyVH(inflater.inflate(R.layout.empty,
                        parent, false));
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE.NORMAL:
                ((UserVH) holder).setUser(members.get(position));
                break;
            case VIEW_TYPE.EMPTY:
                ((EmptyVH) holder).setType(EmptyVH.TYPE.USERS);
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        int count = members == null ? 0 : members.size();
        return count == 0 ? 1 : count;
    }

    @Override
    public int getItemViewType(int position) {
        return members.size() == 0 && position == 0 ? VIEW_TYPE.EMPTY : VIEW_TYPE.NORMAL;
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        User user = dataSnapshot.getValue(User.class);
        members.add(new ExUser(user));
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        User user = dataSnapshot.getValue(User.class);
        user.setId(dataSnapshot.getKey());
        int index = findUserIndex(user.getId());
        if (index != -1) {
            members.remove(index);
            members.add(index, new ExUser(user));
            notifyItemChanged(index);
        }
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        User user = dataSnapshot.getValue(User.class);
        user.setId(dataSnapshot.getKey());
        int index = findUserIndex(user.getId());
        if (index != -1) {
            members.remove(index);
            notifyItemRemoved(index);
        }
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        Log.d(TAG, "child moved");
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Log.d(TAG, "operation cancelled");
    }

    public void registerEventListener() {
        if (shareRef != null)
            shareRef.addChildEventListener(this);
    }

    public void unregisterEventListener() {
        if (shareRef != null)
            shareRef.removeEventListener(this);
    }

    private int findUserIndex(String id) {
        int index = -1;

        if (members != null) {
            int len = members.size();
            for (int i = 0; i < len; i++) {
                if (members.get(i).getId().equals(id)) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public interface VIEW_TYPE {
        int EMPTY = 1;
        int NORMAL = 2;
    }
}
