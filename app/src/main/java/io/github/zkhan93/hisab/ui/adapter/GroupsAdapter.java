package io.github.zkhan93.hisab.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.zkhan93.hisab.R;
import io.github.zkhan93.hisab.model.Group;
import io.github.zkhan93.hisab.model.User;
import io.github.zkhan93.hisab.model.callback.ContextActionBarClbk;
import io.github.zkhan93.hisab.model.callback.GrpSelectModeClbk;
import io.github.zkhan93.hisab.model.callback.OnClickGroupItemClbk;
import io.github.zkhan93.hisab.model.callback.OnLongClickGroupItemClbk;
import io.github.zkhan93.hisab.model.ui.ExGroup;
import io.github.zkhan93.hisab.model.viewholder.EmptyVH;
import io.github.zkhan93.hisab.model.viewholder.GroupItemVH;

/**
 * Created by Zeeshan Khan on 6/26/2016.
 */
public class GroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements
        ChildEventListener, GrpSelectModeClbk, OnClickGroupItemClbk, OnLongClickGroupItemClbk,
        ActionMode.Callback {
    public static final String TAG = GroupsAdapter.class.getSimpleName();
    private List<ExGroup> groups;
    OnClickGroupItemClbk onClickGroupItemClbk;
    private User me;
    private DatabaseReference dbRef;
    private ContextActionBarClbk contextActionBarClbk;
    boolean isMultiMode;
    private int selectedGroupsCount = 0;

    {
        isMultiMode = false;
    }

    private void startMultiSelectMode() {
        //TODO:initialize traking array and start cab
        contextActionBarClbk.showCAB();
        isMultiMode = true;
    }

    public GroupsAdapter(OnClickGroupItemClbk onClickGroupItemClbk, User me, ContextActionBarClbk
            contextActionBarClbk) {
        groups = new ArrayList<>();
        this.onClickGroupItemClbk = onClickGroupItemClbk;
        this.me = me;
        dbRef = FirebaseDatabase.getInstance().getReference("groups/" + me.getId());
        this.contextActionBarClbk = contextActionBarClbk;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE.EMPTY:
                return new EmptyVH(inflater.inflate(R.layout.empty, parent, false));
            default:
                return new GroupItemVH(inflater.inflate(R.layout.group_item, parent, false),
                        this, this);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE.NORMAL) {
            GroupItemVH gHolder = (GroupItemVH) holder;
            gHolder.setGroup(groups.get(position), me);
            if (position == getItemCount() - 1) {
                gHolder.hideDivider();
            }
        }
        if (holder instanceof EmptyVH)
            ((EmptyVH) holder).setType(EmptyVH.TYPE.GROUP);
    }

    @Override
    public int getItemViewType(int position) {
        int count = groups.size();
        if (count == 0)
            return TYPE.EMPTY;
        return TYPE.NORMAL;
    }

    @Override
    public int getItemCount() {
        int count = groups.size();
        return count == 0 ? 1 : count;
    }

    public int findGroupIndex(String id) {
        int index = -1;
        int len = groups.size();
        for (int i = 0; i < len; i++) {
            if (groups.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        Group group = dataSnapshot.getValue(Group.class);
        group.setId(dataSnapshot.getKey());
        groups.add(new ExGroup(group));
        notifyItemInserted(groups.size());
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        Group group = dataSnapshot.getValue(Group.class);
        group.setId(dataSnapshot.getKey());
        int index = findGroupIndex(dataSnapshot.getKey());
        if (index != -1) {
            groups.set(index, new ExGroup(group));
            notifyItemChanged(index);
        }
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        Log.d(TAG, dataSnapshot.toString());
        Group group = dataSnapshot.getValue(Group.class);
        group.setId(dataSnapshot.getKey());
        int index = findGroupIndex(dataSnapshot.getKey());
        if (index != -1) {
            groups.remove(index);
            notifyItemRemoved(index);
        }
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        Log.d(TAG, "onChildMoved");
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Log.d(TAG, "onCancelled");
    }

    public void clear() {
        groups.clear();
        notifyDataSetChanged();
    }

    public void registerChildEventListener() {
        dbRef.orderByChild("name").addChildEventListener(this);
    }

    public void unregisterChildEventListener() {
        dbRef.removeEventListener(this);
    }

    public void sort(int type) {
        switch (type) {
            case Group.SORT_TYPE.ALPHABETICAL:
                Collections.sort(groups, Group.ALPHABETICAL);
                notifyDataSetChanged();
                break;
            case Group.SORT_TYPE.REVERSE_ALPHABETICAL:
                Collections.sort(groups, Group.REVERSE_ALPHABETICAL);
                notifyDataSetChanged();
                break;
            case Group.SORT_TYPE.CHRONOLOGICAL:
                Collections.sort(groups, Group.CHRONOLOGICAL);
                notifyDataSetChanged();
                break;
            case Group.SORT_TYPE.REVERSE_CHRONOLOGICAL:
                Collections.sort(groups, Group.REVERSE_CHRONOLOGICAL);
                notifyDataSetChanged();
                break;
            default:
                Collections.sort(groups, Group.ALPHABETICAL);
                notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void on() {

    }

    @Override
    public void off() {

    }

    @Override
    public void onClick(String groupId, String groupName) {
        if (isMultiMode) {
            int index = findGroupIndex(groupId);
            ExGroup group = groups.get(index);
            if (group.getModerator().getId().equals(me.getId())) {
                if (group.isSelected()) {
                    selectedGroupsCount -= 1;
                } else {
                    selectedGroupsCount += 1;
                }
                group.setSelected(!group.isSelected());
                notifyItemChanged(index);
                contextActionBarClbk.setCount(selectedGroupsCount);
            }
        } else
            onClickGroupItemClbk.onClick(groupId, groupName);
    }

    @Override
    public void onLongClick(String groupId) {
        if (!isMultiMode) {
            startMultiSelectMode();
            //TODO:remove long click listener from each group item
            onClick(groupId, null);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.menu_groups_context, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                Log.d(TAG, "delete");
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        actionMode = null;
        isMultiMode = false;
        int i = 0;
        //clear all selection
        for (ExGroup group : groups) {
            if (group.isSelected()) {
                group.setSelected(false);
                notifyItemChanged(i);
            }
            i++;
        }
        //TODO:add on long click listeners to items
    }

    interface TYPE {
        int EMPTY = 0;
        int NORMAL = 1;
    }

}
