package io.github.zkhan93.hisab.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.zkhan93.hisab.R;
import io.github.zkhan93.hisab.model.Group;
import io.github.zkhan93.hisab.model.User;
import io.github.zkhan93.hisab.model.callback.MembersCountNodesFetchCompleteClbk;
import io.github.zkhan93.hisab.model.callback.UserItemActionClickClbk;
import io.github.zkhan93.hisab.model.ui.ExUser;
import io.github.zkhan93.hisab.ui.adapter.UsersAdapter;
import io.github.zkhan93.hisab.util.Util;

/**
 * A placeholder fragment containing a simple view.
 */
public class ShareFragment extends Fragment implements UserItemActionClickClbk,
        PreferenceChangeListener, TextWatcher, View.OnClickListener {
    public static final String TAG = ShareFragment.class.getSimpleName();

    @BindView(R.id.users)
    RecyclerView usersListView;
    @BindView(R.id.search_text)
    EditText searchText;
    @BindView(R.id.search)
    ImageButton searchBtn;
    Query query;
    private UsersAdapter usersAdapter;
    private String groupId;
    private DatabaseReference shareDbRef;
    private User me;
    private DatabaseReference dbRef;
    private List<User> users = new ArrayList<>();
    ChildEventListener searchedUsers = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Log.d(TAG, "user found:" + dataSnapshot.toString());
            users.add(dataSnapshot.getValue(User.class));
            usersAdapter.setUsers(users);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            Log.d(TAG, "changed:" + dataSnapshot.toString());
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            Log.d(TAG, "removed:" + dataSnapshot.toString());
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            Log.d(TAG, "moved:" + dataSnapshot.toString());
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.d(TAG, "cancelled:" + databaseError.getMessage());
        }
    };

    public ShareFragment() {
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                groupId = bundle.getString("groupId");
                me = bundle.getParcelable("me");
            }
        } else {
            groupId = savedInstanceState.getString("groupId");
            me = savedInstanceState.getParcelable("me");
        }
        shareDbRef = FirebaseDatabase.getInstance().getReference("shareWith/" + groupId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_share, container, false);
        ButterKnife.bind(this, rootView);
        usersListView.setLayoutManager(new LinearLayoutManager(getContext()));
        usersAdapter = new UsersAdapter(this, me, groupId);
        usersListView.setAdapter(usersAdapter);
        searchBtn.setOnClickListener(this);
        getActivity().setTitle(R.string.title_fragment_share);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
//        usersAdapter.registerEventListener();
        searchText.addTextChangedListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
//        usersAdapter.unregisterEventListener();
//        usersAdapter.clear();
        searchText.removeTextChangedListener(this);
    }

    @Override
    public void userClicked(final ExUser user) {
        updateMembersCount(new MembersCountNodesFetchCompleteClbk() {
            @Override
            public void onMembersCountNodeComplete(final List<String> memberCountNodes) {
                if (user.isChecked()) {
                    Log.d(TAG, "adding " + user.getName() + " to share list ");

                    dbRef.child("groups").child(me.getId()).child(groupId).addListenerForSingleValueEvent
                            (new ValueEventListener() {

                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    //add the groups in user's(the user clicked) list
                                    Group group = dataSnapshot.getValue(Group.class);
                                    group.setMembersCount(memberCountNodes.size() + 1);
                                    Map<String, Object> map = new HashMap<>();
                                    for (String s : memberCountNodes) {
                                        map.put(s, memberCountNodes.size() + 1);
                                    }
                                    map.put("groups/" + user.getId() + "/" + dataSnapshot.getKey
                                            (), group.toMap());
                                    map.put("shareWith/" + groupId + "/" + user.getId(), new User(user.getName(), user.getEmail(), user
                                            .getId()).toMap());
                                    Log.d(TAG, map.toString());
                                    //removing children of above node
                                    dbRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                            if (databaseError != null)
                                                Log.d(TAG, "sharing with user" + user.getName() + " " +
                                                        "failed");
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.d(TAG, "group fetching onCancelled");
                                }
                            });

                } else {
                    Log.d(TAG, "removing " + user.getName() + " from sharing list");
                    Map<String, Object> map = new HashMap<>();
                    for (String s : memberCountNodes) {
                        map.put(s, memberCountNodes.size() - 1);
                    }
                    map.put("shareWith/" + groupId + "/" + user.getId(), null);
                    map.put("groups/" + user.getId() + "/" + groupId, null);
                    //remove children node of above end
                    map.remove("/groups/" + user.getId() + "/" + groupId + "/membersCount");
                    //shareDbRef.child(user.getId()).removeValue();
                    //child("groups").child(user.getId()).child(groupId).removeValue(
                    dbRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null)
                                Log.d(TAG, "removing sharing with user" + user.getName() + " " +
                                        "failed");
                        }
                    });
                }
            }
        });
    }

    private void updateMembersCount(final MembersCountNodesFetchCompleteClbk
                                            membersCountNodesFetchCompleteClbk) {
        //update the member count for this group in all its copies

        ValueEventListener valueEventListener = new ValueEventListener() {
            List<String> nodes = new ArrayList<>();

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                int membersCount = (int) dataSnapshot.getChildrenCount() + 1;
                nodes.add("/groups/" + me.getId() + "/" + groupId +
                        "/membersCount");
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    nodes.add("/groups/" + ds.getValue(User.class).getId() + "/" +
                            groupId +
                            "/membersCount");
                }
                Log.d(TAG, "nodes to update are" + nodes.toString());
                membersCountNodesFetchCompleteClbk.onMembersCountNodeComplete(nodes);
//                        dbRef.updateChildren(map, new DatabaseReference.CompletionListener() {
//                            @Override
//                            public void onComplete(DatabaseError databaseError, DatabaseReference
//                                    databaseReference) {
//                                if (databaseError != null)
//                                    Log.d(TAG, "share error: " + databaseError.getMessage());
//                                else Log.d(TAG, "share successful");
//                            }
//                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "group share list fetching onCancelled");
            }
        };
        dbRef.child("shareWith").child(groupId)
                .addListenerForSingleValueEvent(valueEventListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("groupId", groupId);
        outState.putParcelable("me", me);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent preferenceChangeEvent) {
        String keyChanged = preferenceChangeEvent.getKey();
        if (keyChanged.equals("name") || keyChanged.equals("email") || keyChanged.equals("user_id")) {
            me = Util.getUser(getActivity());
        }
    }

    private void searchForUser(String searchText) {
        users.clear();
        usersAdapter.setUsers(users);
        searchText = searchText.trim();
        if (searchText.isEmpty() || searchText.length() < 4)
            return;
        if (query != null)
            query.removeEventListener(searchedUsers);

        query = dbRef.child("users").orderByChild("email").startAt(searchText).endAt(searchText + "~");
        query.addChildEventListener(searchedUsers);
    }

    /**
     * OnClickListener
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.search:
                searchForUser(searchText.getText().toString());
                break;
            default:
                Log.d(TAG, "click not implemented");
        }
    }

    /**
     * TextWatcher implementation
     */
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        searchForUser(editable.toString());
    }
}
