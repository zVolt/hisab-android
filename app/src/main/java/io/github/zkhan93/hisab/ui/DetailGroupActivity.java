package io.github.zkhan93.hisab.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.zkhan93.hisab.R;
import io.github.zkhan93.hisab.model.ExpenseItem;
import io.github.zkhan93.hisab.ui.dialog.CreateExpenseItemDialog;

public class DetailGroupActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = DetailGroupActivity.class.getSimpleName();

    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    DatabaseReference dbRef;
    private String groupId, groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_group);
        ButterKnife.bind(this);
        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");
        dbRef = FirebaseDatabase.getInstance().getReference("expenses/" + groupId);
        setSupportActionBar(toolbar);
        fab.setOnClickListener(this);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag
                (DetailGroupActivityFragment.TAG);
        if (fragment == null)
            fragment = new DetailGroupActivityFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, fragment,
                DetailGroupActivityFragment.TAG).commit();
        setTitle(groupName);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                showAddExpenseView();
                break;
            default:
                Log.d(TAG, "click not implemented");
        }
    }

    private void showAddExpenseView() {
        DialogFragment dialog = new CreateExpenseItemDialog();
        dialog.show(getFragmentManager(), "dialog");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    public void createExpense(String description, float amount) {
        ExpenseItem expenseItem = new ExpenseItem(description, amount);
        expenseItem.setCreatedOn(Calendar.getInstance().getTimeInMillis());
        dbRef.push().setValue(expenseItem);
    }
}
