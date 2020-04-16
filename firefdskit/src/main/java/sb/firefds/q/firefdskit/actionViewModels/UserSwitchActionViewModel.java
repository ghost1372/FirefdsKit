package sb.firefds.q.firefdskit.actionViewModels;

import android.content.Intent;

import sb.firefds.q.firefdskit.R;

import static sb.firefds.q.firefdskit.XSysUIGlobalActions.getResources;
import static sb.firefds.q.firefdskit.utils.Constants.MULTIUSER_ACTION;
import static sb.firefds.q.firefdskit.utils.Packages.SETTINGS;

public class UserSwitchActionViewModel extends FirefdsKitActionViewModel {

    UserSwitchActionViewModel() {

        super();
        getActionInfo().setName(MULTIUSER_ACTION);
        getActionInfo().setLabel(getResources().getString(R.string.switchUser));
        setDrawableIcon(getResources().getDrawable(R.drawable.tw_ic_do_users_stock, null));
    }

    @Override
    public void onPress() {

        getmGlobalActions().dismissDialog(false);
        showUserSwitchScreen();
    }

    @Override
    public void onPressSecureConfirm() {
        showUserSwitchScreen();
    }

    private void showUserSwitchScreen() {
        Intent rebootIntent = new Intent("android.settings.USER_SETTINGS")
                .setPackage(SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getmContext().startActivity(rebootIntent);
    }
}
