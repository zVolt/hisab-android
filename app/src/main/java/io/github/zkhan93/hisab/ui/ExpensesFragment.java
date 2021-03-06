package io.github.zkhan93.hisab.ui;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.zkhan93.hisab.R;
import io.github.zkhan93.hisab.model.ExpenseItem;
import io.github.zkhan93.hisab.model.User;
import io.github.zkhan93.hisab.model.callback.ExpenseItemClbk;
import io.github.zkhan93.hisab.model.callback.GroupRenameClbk;
import io.github.zkhan93.hisab.model.callback.ShowMessageClbk;
import io.github.zkhan93.hisab.model.callback.SummaryActionItemClbk;
import io.github.zkhan93.hisab.ui.adapter.ExpensesAdapter;
import io.github.zkhan93.hisab.ui.dialog.CashItemDialog;
import io.github.zkhan93.hisab.ui.dialog.ConfirmDialog;
import io.github.zkhan93.hisab.ui.dialog.EditCashItemDialog;
import io.github.zkhan93.hisab.ui.dialog.EditExpenseItemDialog;
import io.github.zkhan93.hisab.ui.dialog.ExpenseItemDialog;
import io.github.zkhan93.hisab.ui.dialog.RenameGroupDialog;
import io.github.zkhan93.hisab.util.ImagePicker;
import io.github.zkhan93.hisab.util.Util;

/**
 * A placeholder fragment containing a simple view.
 */
public class ExpensesFragment extends Fragment implements ValueEventListener,
        PreferenceChangeListener, View.OnClickListener, GroupRenameClbk, ExpenseItemClbk, Toolbar
                .OnMenuItemClickListener {
    public static final String TAG = ExpensesFragment.class.getSimpleName();
    //member views
    @BindView(R.id.expenses)
    RecyclerView expensesList;

    @BindView(R.id.fabMenu)
    FloatingActionMenu fabMenu;

    @BindView(R.id.fabCreateShared)
    FloatingActionButton fabShareEntry;

    @BindView(R.id.fabCreatePaidReceived)
    FloatingActionButton fabGiveTakeEntryEntry;

    private Toolbar toolbar;
    //other members
    private String groupId, groupName;
    private ExpensesAdapter expensesAdapter;
    private User me;
    private DatabaseReference groupNameRef, groupExpensesRef;
    private DatabaseReference dbRef;

    private ShowMessageClbk showMessageClbk;
    private boolean isTwoPaneMode;

    public ExpensesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle bundle = getArguments();
            if (bundle != null) {
                groupId = bundle.getString("groupId");//TODO:send this via arguments and update
                // this on every group click
                groupName = bundle.getString("groupName");
            }
        } else {
            groupId = savedInstanceState.getString("groupId");
            groupName = savedInstanceState.getString("groupName");
        }
        me = Util.getUser(getContext());
        dbRef = FirebaseDatabase.getInstance().getReference();
//        changeGroup(groupId);
        groupNameRef = dbRef.child("groups").child(me.getId()).child(groupId).child("name");
        groupExpensesRef = dbRef.child("expenses").child(groupId);
        showMessageClbk = getActivity() instanceof MainActivity ? (ShowMessageClbk) getActivity()
                : null;
        Util.deleteNotifications(getActivity().getApplicationContext(), groupId);

    }

    public void changeGroup(String groupId) {
        groupNameRef.removeEventListener(this);
        groupNameRef = dbRef.child("groups").child(me.getId()).child(groupId).child("name");
        groupExpensesRef = dbRef.child("expenses").child(groupId);
        if (expensesAdapter == null)
            expensesAdapter = new ExpensesAdapter(me, groupId, this, (SummaryActionItemClbk)
                    getActivity());
        else
            expensesAdapter.changeGroup(groupId);
        if (expensesList != null)
            expensesList.setAdapter(expensesAdapter);
        if (groupNameRef != null)
            groupNameRef.addValueEventListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_expenses, container, false);
        ButterKnife.bind(this, rootView);
        expensesList.setLayoutManager(new LinearLayoutManager(getActivity()));

        ExpenseItemDecoration expenseItemDecoration = new ExpenseItemDecoration(getResources(), R
                .drawable.horizontal_divider, getActivity().getTheme());
        expensesList.addItemDecoration(expenseItemDecoration);
        expensesList.setItemAnimator(new DefaultItemAnimator());

        expensesAdapter = new ExpensesAdapter(me, groupId, this, (SummaryActionItemClbk)
                getActivity());
        expensesList.setAdapter(expensesAdapter);
        isTwoPaneMode = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean
                ("isTwoPaneMode", false);
        if (!isTwoPaneMode) {
            setHasOptionsMenu(true);
            getActivity().setTitle(groupName);
        } else {
            toolbar = ButterKnife.findById(getActivity(), R.id.toolbar_expenses);
            if (toolbar != null) {
                toolbar.inflateMenu(R.menu.menu_detail_group);
                toolbar.setTitle(groupName);
                toolbar.setOnMenuItemClickListener(this);
            }
        }

        fabMenu.setClosedOnTouchOutside(true);
        fabShareEntry.setOnClickListener(this);
        fabGiveTakeEntryEntry.setOnClickListener(this);
        hideFabMenu();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail_group, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_share:
                showShareGroupUi();
                return true;
            case R.id.action_rename:
                showRenameUi();
                return true;
            case R.id.action_info:

                return true;
            default:
                return false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("groupId", groupId);
        outState.putString("groupName", groupName);
    }

    @Override
    public void onStart() {
        super.onStart();
        expensesAdapter.registerEventListener();
        groupNameRef.addValueEventListener(this);

        fabMenu.postDelayed(new Runnable() {
            @Override
            public void run() {
                fabMenu.showMenu(true);
            }
        }, 400);

//        int cx = fabMenu.getRight();
//        int cy = fabMenu.getBottom();
//        int finalRadius = Math.max(fabMenu.getWidth(), fabMenu.getHeight());
//        Animator anim = ViewAnimationUtils.createCircularReveal(fabMenu, cx, cy, 0, finalRadius);
//        fabMenu.setVisibility(View.VISIBLE);
//        anim.setDuration(1000);
//        anim.setInterpolator(new AccelerateInterpolator());
//        anim.start();
    }

    public void hideFabMenu() {
        fabMenu.hideMenu(true);
    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit
                ().putLong(groupId, Calendar.getInstance().getTimeInMillis()).apply();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        expensesAdapter.unregisterEventListener();
        expensesAdapter.clear();
        groupNameRef.removeEventListener(this);
    }


    private void showRenameUi() {
        RenameGroupDialog renameDialog = new RenameGroupDialog();
        Bundle bundle = new Bundle();
        bundle.putString("name", groupName);
        renameDialog.setArguments(bundle);
        renameDialog.show(getActivity().getFragmentManager(), RenameGroupDialog.TAG);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot != null && dataSnapshot.getValue(String.class) != null)
            groupName = dataSnapshot.getValue(String.class);
        if (isVisible()) {
            if (!isTwoPaneMode)
                getActivity().setTitle(groupName);
            else if (toolbar != null)
                toolbar.setTitle(groupName);
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Log.d(TAG, "name fetching operation cancelled");
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent preferenceChangeEvent) {
        String keyChanged = preferenceChangeEvent.getKey();
        if (keyChanged.equals("name") || keyChanged.equals("email") || keyChanged.equals
                ("user_id")) {
            me = Util.getUser(getActivity());
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fabCreateShared:
                showAddExpenseView();
                fabMenu.close(false);
                break;
            case R.id.fabCreatePaidReceived:
                showAddGiveTakeEntryView();
                fabMenu.close(false);
                break;
            default:
                Log.d(TAG, "click not implemented");
        }
    }


    private void showAddGiveTakeEntryView() {
        DialogFragment dialog = new CashItemDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable("me", me);
        bundle.putString("groupId", groupId);
        dialog.setArguments(bundle);
//        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
//        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        dialog.show(getActivity().getSupportFragmentManager(), CashItemDialog.TAG);
    }

    private void showAddExpenseView() {
        android.support.v4.app.DialogFragment dialog = new ExpenseItemDialog();
//        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
//        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        dialog.show(getActivity().getSupportFragmentManager(), ExpenseItemDialog.TAG);
    }

    @Override
    public void renameTo(String newName) {
        if (newName == null || newName.isEmpty()) {
            Log.e(TAG, "invalid new name, cannot rename groups");
            return;
        }
        if (me == null || me.getId() == null || me.getId().isEmpty()) {
            Log.e(TAG, "user id is not valid cannot rename group");
            return;
        }
        if (groupId == null || groupId.isEmpty()) {
            Log.e(TAG, "groupId is not valid cannot rename group");
            return;
        }
        attemptRename(newName);
    }

    private void attemptRename(final String newName) {
        dbRef.child("shareWith").child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            ArrayList<String> sharedUserIds = new ArrayList<>();

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot dbs : dataSnapshot.getChildren()) {
                    sharedUserIds.add(dbs.getKey());
                }
                //get moderators groups name's reference
                dbRef.child("groups").child(me.getId()).child(groupId).child("moderator").child
                        ("id")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                sharedUserIds.add(dataSnapshot.getValue(String.class));
                                //update all group entries
                                Map<String, Object> updateLocation = new HashMap<>();
                                long now = Calendar.getInstance().getTimeInMillis();
                                for (String userId : sharedUserIds) {
                                    updateLocation.put("/groups/" + userId + "/" + groupId +
                                            "/name", newName);
                                    updateLocation.put("/groups/" + userId + "/" + groupId +
                                            "/updatedOn", now);
                                }
                                dbRef.updateChildren(updateLocation, new DatabaseReference
                                        .CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError,
                                                           DatabaseReference
                                                                   databaseReference) {
                                        if (databaseError != null)
                                            Log.d(TAG, "Error occurred" + databaseError
                                                    .getMessage());
                                    }
                                });
                                groupNameRef.setValue(newName);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.e(TAG, "error fetching shared with user ids");
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "error fetching shared with user ids");
            }
        });
    }

    /**
     * Called from {@link ExpenseItemDialog} and {@link CashItemDialog}'s positive
     * button's [@link OnClickListener]
     *
     * @param description decription of expense to create
     * @param amount      amount
     */
    public void createExpense(String description, float amount, int itemType, User with, int
            shareType) {
        ExpenseItem expenseItem;
        if (itemType == ExpenseItem.ITEM_TYPE.PAID_RECEIVED)
            expenseItem = new ExpenseItem(description, amount, with, shareType);
        else
            expenseItem = new ExpenseItem(description, amount);
        expenseItem.setCreatedOn(Calendar.getInstance().getTimeInMillis());
        expenseItem.setOwner(me);
//        expenseItem.setGroupId(groupId); no need to set this as data is already under the group
// id branch
        groupExpensesRef.push().setValue(expenseItem).addOnCompleteListener(getActivity(), new
                OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "";
                        if (!task.isSuccessful()) {
                            msg = new StringBuilder("Error occurred: ").append(task.getException()
                                    .getLocalizedMessage()).toString();
                            showMessageClbk.showMessage(msg, ShowMessageClbk.TYPE.SNACKBAR,
                                    Snackbar.LENGTH_INDEFINITE);
                        }

                    }
                });
    }

    public void showShareGroupUi() {
        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag
                (ShareFragment
                        .TAG);
        if (fragment == null) {
            fragment = new ShareFragment();
            Bundle bundle = new Bundle();
            bundle.putString("groupId", groupId);
            bundle.putParcelable("me", me);
            fragment.setArguments(bundle);
        }
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer,
                fragment,
                ShareFragment.TAG).addToBackStack(ExpensesFragment.TAG).commit();
        fabMenu.hideMenu(true);
//        getActivity().setTitle("Share with");
    }

    /**
     * {@link ExpenseItemClbk} implementations
     */
    @Override
    public void showEditUi(ExpenseItem expense) {
        switch (expense.getItemType()) {
            case ExpenseItem.ITEM_TYPE.SHARED:
                EditExpenseItemDialog dialog = new EditExpenseItemDialog();
                Bundle bundle = new Bundle();
                bundle.putParcelable("expense", expense);
                dialog.setArguments(bundle);
                dialog.show(getActivity().getSupportFragmentManager(), EditExpenseItemDialog.TAG);
                break;
            case ExpenseItem.ITEM_TYPE.PAID_RECEIVED:
                EditCashItemDialog pdialog = new EditCashItemDialog();
                Bundle pbundle = new Bundle();
                pbundle.putParcelable("expense", expense);
                pbundle.putString("groupId", groupId);
                pbundle.putParcelable("me", me);
                pdialog.setArguments(pbundle);
                pdialog.show(getActivity().getFragmentManager(), EditCashItemDialog.TAG);
                break;
            default:
                Log.d(TAG, "trying to edit a invalid expense item");
        }
    }

    @Override
    public void deleteExpense(final String expenseId) {
        Bundle bundle = new Bundle();
        bundle.putInt("type", ConfirmDialog.TYPE.EXPENSE_DELETE);
        bundle.putString("msg", "Do you really want to delete?");//TODO: String resource
        bundle.putString("positiveBtnTxt", "Yes");//TODO: String resource
        bundle.putString("negativeBtnTxt", "No");//TODO: String resource
        ((MainActivity) getActivity()).setToDeleteExpenseId(expenseId);
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setArguments(bundle);
        confirmDialog.show(getActivity().getFragmentManager(), ConfirmDialog.TAG);
    }

    @Override
    public void update(final ExpenseItem expense, boolean imageChanged, boolean imageAdded) {
        // prevent extra data from getting send to firebase
        final String expenseId = expense.getId();
        Log.d(TAG, "expense to udate: " + expense.toJson());
        groupExpensesRef.child(expenseId)
                .setValue(expense)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {

                            String msg = "Error Occurred:";
                            if (task.getException() != null) {
                                Log.d(TAG, "exception upating expense", task.getException());
                                msg += task.getException().getLocalizedMessage();
                            }
                            showMessageClbk.showMessage(msg, ShowMessageClbk.TYPE.SNACKBAR,
                                    Snackbar.LENGTH_INDEFINITE);
                        }
                    }
                });

        if (imageChanged) {
            if (imageAdded) {
                Uri imageUri = ImagePicker.getSelectedImageUri();
                if (imageUri != null) {
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                    String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType
                            (getActivity().getContentResolver().getType(imageUri)); // extension
                    // without .

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImagePicker.getSelectedBitmapImage().compress(Bitmap.CompressFormat.JPEG, 100,
                            baos);
                    byte[] data = baos.toByteArray();

                    storageRef.child("expenses").child(expenseId + '.' + extension)
                            .putBytes(data)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Log.d(TAG, "uploaded");
                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                    groupExpensesRef.child(expenseId).child("image").setValue
                                            (downloadUrl.toString());
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "failed image upload");
                        }
                    });

                }
            } else {
                groupExpensesRef.child(expenseId).child("image").setValue(null);
            }
        }

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }
}
