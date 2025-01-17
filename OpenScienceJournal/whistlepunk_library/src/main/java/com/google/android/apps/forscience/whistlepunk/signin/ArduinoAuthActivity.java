package com.google.android.apps.forscience.whistlepunk.signin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.apps.forscience.auth0.Auth0Token;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.accounts.arduino.ArduinoAccount;
import com.google.android.apps.forscience.whistlepunk.gdrivesync.GDriveSyncSetupActivity;

import java.util.ArrayList;
import java.util.List;

public class ArduinoAuthActivity extends AppCompatActivity implements AuthBaseFragment.Listener {

    private static final String LOG_TAG = "ArduinoSignInActivity";

    private boolean mBlocked;

    private View mBack;

    private View mBlocker;

    private FragmentManager mFragmentManager;

    private List<AuthBaseFragment> mFragments;

    private List<Fragment.SavedState> mFragmentSavedStates;

    private boolean mBackEnabled;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_auth);
        findViewById(R.id.auth_header_action_close).setOnClickListener(v -> finish());
        mBack = findViewById(R.id.auth_header_action_back);
        mBack.setOnClickListener(v -> onBackPressed());
        mBlocker = findViewById(R.id.blocker);
        findViewById(R.id.auth_terms).setOnClickListener(v -> Commons.openTerms(this));
        findViewById(R.id.auth_privacy).setOnClickListener(v -> Commons.openPrivacy(this));
        mFragmentManager = getSupportFragmentManager();
        mFragments = new ArrayList<>();
        mFragmentSavedStates = new ArrayList<>();
        setFirstFragment(new AccountTypeFragment());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (!goBack()) {
            finish();
        }
    }

    @Override
    public void onStartFragment(Class<? extends AuthBaseFragment> cls, Bundle args) {
        try {
            final int fragmentCount = mFragments.size();
            if (fragmentCount > 0) {
                final AuthBaseFragment currentFragment = mFragments.get(fragmentCount - 1);
                final Fragment.SavedState state = mFragmentManager.saveFragmentInstanceState(currentFragment);
                mFragmentSavedStates.add(state);
            }
            AuthBaseFragment fragment = cls.newInstance();
            if (args != null) {
                fragment.setArguments(args);
            }
            mFragments.add(fragment);
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.slide_horizontal_in, R.anim.slide_horizontal_out, R.anim.slide_horizontal_out_reverse, R.anim.slide_horizontal_in_reverse);
            fragmentTransaction.replace(R.id.fragment, fragment);
            fragmentTransaction.commitAllowingStateLoss();
            refreshBackStatusUI();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to attach Fragment to Activity", e);
        }
    }

    @Override
    public void onBlockUI(boolean block) {
        if (mBlocked != block) {
            mBlocker.setVisibility(block ? View.VISIBLE : View.GONE);
            mBlocker.setKeepScreenOn(block);
            mBlocked = block;
        }
    }

    @Override
    public void onBackEnabled(boolean enabled) {
        mBackEnabled = enabled;
        refreshBackStatusUI();
    }

    @Override
    public void onBack() {
        goBack();
    }

    @Override
    public void onBegin() {
        goBegin();
    }

    @Override
    public void onClose() {
        finish();
    }

    @Override
    public void onAuthCompleted(final Auth0Token token) {
        final Intent data = new Intent();
        data.putExtra("token", token);
        setResult(RESULT_OK, data);
        finish();
        final ArduinoAccount account = new ArduinoAccount(this, token);
        if (!account.isMinor()) {
            startActivity(new Intent(this, GDriveSyncSetupActivity.class));
        }
    }

    private void setFirstFragment(AuthBaseFragment fragment) {
        mFragments.clear();
        mFragmentSavedStates.clear();
        mFragments.add(fragment);
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment, fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private boolean goBack() {
        if (mBlocked || !mBackEnabled) {
            return true;
        }
        final int fragmentsCount = mFragments.size();
        if (fragmentsCount > 0) {
            final AuthBaseFragment currentFragment = mFragments.get(fragmentsCount - 1);
            if (currentFragment.onBackPressed()) {
                return true;
            }
        }
        if (fragmentsCount > 1) {
            mFragments.remove(fragmentsCount - 1);
            final AuthBaseFragment previousFragment = mFragments.get(fragmentsCount - 2);
            final Fragment.SavedState previousState = mFragmentSavedStates.remove(fragmentsCount - 2);
            previousFragment.setInitialSavedState(previousState);
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.slide_horizontal_out_reverse, R.anim.slide_horizontal_in_reverse, R.anim.slide_horizontal_in, R.anim.slide_horizontal_out);
            fragmentTransaction.replace(R.id.fragment, previousFragment);
            fragmentTransaction.commitAllowingStateLoss();
            refreshBackStatusUI();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean goBegin() {
        if (mBlocked) {
            return true;
        }
        final int fragmentsCount = mFragments.size();
        if (fragmentsCount > 1) {
            while (mFragments.size() > 1) {
                mFragments.remove(1);
            }
            while (mFragmentSavedStates.size() > 1) {
                mFragmentSavedStates.remove(1);
            }
            final AuthBaseFragment previousFragment = mFragments.get(0);
            final Fragment.SavedState previousState = mFragmentSavedStates.remove(0);
            previousFragment.setInitialSavedState(previousState);
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.slide_horizontal_out_reverse, R.anim.slide_horizontal_in_reverse, R.anim.slide_horizontal_in, R.anim.slide_horizontal_out);
            fragmentTransaction.replace(R.id.fragment, previousFragment);
            fragmentTransaction.commitAllowingStateLoss();
            refreshBackStatusUI();
            return true;
        }
        return false;
    }

    private void refreshBackStatusUI() {
        if (mBackEnabled && mFragments.size() > 1) {
            mBack.setVisibility(View.VISIBLE);
        } else {
            mBack.setVisibility(View.INVISIBLE);
        }
    }

}
