/**
    Copyright (C) 2020 Forrest Guice
    This file is part of SuntimesWidget.

    SuntimesWidget is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SuntimesWidget is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SuntimesWidget.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.forrestguice.suntimeswidget.alarmclock.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.transition.TransitionInflater;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.forrestguice.suntimeswidget.R;
import com.forrestguice.suntimeswidget.SuntimesUtils;
import com.forrestguice.suntimeswidget.alarmclock.AlarmClockItem;
import com.forrestguice.suntimeswidget.alarmclock.AlarmNotifications;
import com.forrestguice.suntimeswidget.settings.AppSettings;

@SuppressWarnings("Convert2Diamond")
public class AlarmEditDialog extends DialogFragment
{
    public static final String EXTRA_SHOW_FRAME = "show_frame";
    public static final String EXTRA_SHOW_OVERFLOW = "show_overflow";

    protected View dialogFrame;
    protected TextView text_title;
    protected AlarmClockItem item = null, original = null;
    protected AlarmEditViewHolder itemView;

    public AlarmEditDialog()
    {
        super();
        setArguments(new Bundle());
    }

    public void initFromItem(AlarmClockItem item, boolean addItem)
    {
        this.original = (addItem ? null : item);
        this.item = new AlarmClockItem(item);
        this.item.modified = false;
        bindItemToHolder(item);
    }
    public AlarmClockItem getItem() {
        return item;
    }
    public AlarmClockItem getOriginal() {
        return original;
    }

    public boolean isModified() {
        return ((item != null && item.modified) || original == null);
    }

    public void notifyItemChanged() {
        item.modified = true;
        bindItemToHolder(item);
    }

    protected void bindItemToHolder(AlarmClockItem item)
    {
        if (itemView != null)
        {
            detachClickListeners(itemView);
            itemView.bindDataToPosition(getActivity(), item, 0);
            itemView.menu_overflow.setVisibility(getArguments().getBoolean(EXTRA_SHOW_OVERFLOW, true) ? View.VISIBLE : View.GONE);
            attachClickListeners(itemView, 0);
        }
        if (text_title != null) {
            text_title.setText(item != null ? item.type.getDisplayString() : "");
        }
    }

    @Override
    public void onCreate(Bundle savedState)
    {
        super.onCreate(savedState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setSharedElementEnterTransition(TransitionInflater.from(getContext()).inflateTransition(android.R.transition.move));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedState)
    {
        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(getActivity(), AppSettings.loadTheme(getContext()));
        View dialogContent = inflater.cloneInContext(contextWrapper).inflate(R.layout.layout_dialog_alarmitem, parent, false);

        initViews(getContext(), dialogContent);
        if (savedState != null) {
            loadSettings(savedState);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)        // transition animation
        {
            if (item != null && itemView != null) {
                itemView.text_datetime.setTransitionName("transition_" + item.rowID);
                startPostponedEnterTransition();
            }
        }

        return dialogContent;
    }

    protected void initViews(Context context, View dialogContent)
    {
        itemView = new AlarmEditViewHolder(dialogContent);
        text_title = (TextView) dialogContent.findViewById(R.id.dialog_title);

        ImageButton btn_cancel = (ImageButton) dialogContent.findViewById(R.id.dialog_button_cancel);
        if (btn_cancel != null) {
            btn_cancel.setOnClickListener(onDialogCancelClick);
        }

        ImageButton btn_accept = (ImageButton) dialogContent.findViewById(R.id.dialog_button_accept);
        if (btn_accept != null) {
            btn_accept.setOnClickListener(onDialogAcceptClick);
        }

        Button btn_neutral = (Button) dialogContent.findViewById(R.id.dialog_button_neutral);
        if (btn_neutral != null) {
            btn_neutral.setOnClickListener(onDialogNeutralClick);
        }

        dialogFrame = dialogContent.findViewById(R.id.dialog_frame);
        setShowDialogFrame(getArguments().getBoolean(EXTRA_SHOW_FRAME, true));

        bindItemToHolder(item);
    }

    public void setShowOverflow(boolean value)
    {
        getArguments().putBoolean(EXTRA_SHOW_OVERFLOW, value);
        bindItemToHolder(getItem());
    }

    public void setShowDialogFrame(boolean value)
    {
        getArguments().putBoolean(EXTRA_SHOW_FRAME, value);
        if (dialogFrame != null) {
            dialogFrame.setVisibility(value ? View.VISIBLE : View.GONE);
        }
    }

    @SuppressWarnings({"deprecation","RestrictedApi"})
    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(onDialogShow);
        return dialog;
    }

    @Override
    public void onSaveInstanceState( Bundle outState )
    {
        saveSettings(outState);
        super.onSaveInstanceState(outState);
    }

    protected void loadSettings(Bundle bundle)
    {
        this.item = bundle.getParcelable("item");
        this.original = bundle.getParcelable("original");
        bindItemToHolder(item);
    }

    protected void saveSettings(Bundle bundle)
    {
        bundle.putParcelable("item", item);
        bundle.putParcelable("original", original);
    }

    private DialogInterface.OnClickListener onAccepted = null;
    public void setOnAcceptedListener( DialogInterface.OnClickListener listener ) {
        onAccepted = listener;
    }

    private DialogInterface.OnClickListener onCanceled = null;
    public void setOnCanceledListener( DialogInterface.OnClickListener listener ) {
        onCanceled = listener;
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    private DialogInterface.OnShowListener onDialogShow = new DialogInterface.OnShowListener()
    {
        @Override
        public void onShow(DialogInterface dialog) {
            // EMPTY; placeholder
        }
    };

    private View.OnClickListener onDialogNeutralClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // TODO: neutral click
        }
    };

    private View.OnClickListener onDialogCancelClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Dialog dialog = getDialog();
            if (dialog != null) {
                dialog.cancel();
            }
        }
    };

    @Override
    public void onCancel(DialogInterface dialog)
    {
        if (onCanceled != null) {
            onCanceled.onClick(getDialog(), 0);
        }
    }

    private View.OnClickListener onDialogAcceptClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (onAccepted != null) {
                onAccepted.onClick(getDialog(), 0);
            }
            dismiss();
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    protected void showAlarmTypeMenu(Context context, final AlarmClockItem item, final View buttonView)
    {
        PopupMenu menu = new PopupMenu(context, buttonView);
        MenuInflater inflater = menu.getMenuInflater();
        inflater.inflate(R.menu.alarmtype, menu.getMenu());

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                switch (menuItem.getItemId())
                {
                    case R.id.alarmTypeNotification:
                        item.type = AlarmClockItem.AlarmType.NOTIFICATION;
                        if (listener != null) {
                            listener.onTypeChanged(item);
                        }
                        notifyItemChanged();
                        return true;

                    case R.id.alarmTypeAlarm:
                    default:
                        item.type = AlarmClockItem.AlarmType.ALARM;
                        if (listener != null) {
                            listener.onTypeChanged(item);
                        }
                        notifyItemChanged();
                        return true;
                }
            }
        });

        SuntimesUtils.forceActionBarIcons(menu.getMenu());
        menu.show();
    }

    protected void showOverflowMenu(final Context context, final AlarmClockItem item, final View buttonView)
    {
        PopupMenu menu = new PopupMenu(context, buttonView);
        MenuInflater inflater = menu.getMenuInflater();
        inflater.inflate(R.menu.alarmcontext1, menu.getMenu());

        if (Build.VERSION.SDK_INT < 11)     // TODO: add support for api10
        {
            MenuItem[] notSupportedMenuItems = new MenuItem[] {     // not supported by api level
                    menu.getMenu().findItem(R.id.setAlarmTime),
                    menu.getMenu().findItem(R.id.setAlarmOffset)
            };
            for (MenuItem menuItem : notSupportedMenuItems) {
                menuItem.setEnabled(false);
            }
        }

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                switch (menuItem.getItemId())
                {
                    case R.id.deleteAlarm:
                        confirmDeleteAlarm(getActivity(), item, onDeleteConfirmed(item));
                        return true;

                    default:
                        return false;
                }
            }
        });

        SuntimesUtils.forceActionBarIcons(menu.getMenu());
        menu.show();
    }

    public static void confirmDeleteAlarm(final Context context, final AlarmClockItem item, DialogInterface.OnClickListener onDeleteConfirmed)
    {
        String message = context.getString(R.string.deletealarm_dialog_message, AlarmEditViewHolder.displayAlarmLabel(context, item), AlarmEditViewHolder.displayAlarmTime(context, item), AlarmEditViewHolder.displayEvent(context, item));
        AlertDialog.Builder confirm = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.deletealarm_dialog_title)).setMessage(message).setIcon(R.drawable.ic_action_discard)
                .setPositiveButton(context.getString(R.string.deletealarm_dialog_ok), onDeleteConfirmed)
                .setNegativeButton(context.getString(R.string.deletealarm_dialog_cancel), null);
        confirm.show();
    }

    protected DialogInterface.OnClickListener onDeleteConfirmed( final AlarmClockItem item ) {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                getActivity().sendBroadcast(AlarmNotifications.getAlarmIntent(getActivity(), AlarmNotifications.ACTION_DELETE, item.getUri()));
                dialog.cancel();
            }
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void attachClickListeners(@NonNull AlarmEditViewHolder holder, int position)
    {
        holder.menu_type.setOnClickListener(showAlarmTypeMenu());
        holder.menu_overflow.setOnClickListener(showOverflowMenu());
        holder.edit_label.setOnClickListener(pickLabel());
        holder.chip_offset.setOnClickListener(pickOffset());
        holder.chip_event.setOnClickListener(pickEvent());
        holder.chip_location.setOnClickListener(pickLocation());
        holder.chip_repeat.setOnClickListener(pickRepeating());
        holder.chip_ringtone.setOnClickListener(pickRingtone());
        holder.check_vibrate.setOnCheckedChangeListener(pickVibrating());
        holder.chip_action0.setOnClickListener(pickAction(0));
        holder.chip_action1.setOnClickListener(pickAction(1));
    }

    private void detachClickListeners(@NonNull AlarmEditViewHolder holder)
    {
        holder.menu_type.setOnClickListener(null);
        holder.menu_overflow.setOnClickListener(null);
        holder.edit_label.setOnClickListener(null);
        holder.chip_offset.setOnClickListener(null);
        holder.chip_event.setOnClickListener(null);
        holder.chip_location.setOnClickListener(null);
        holder.chip_repeat.setOnClickListener(null);
        holder.chip_ringtone.setOnClickListener(null);
        holder.check_vibrate.setOnCheckedChangeListener(null);
        holder.chip_action0.setOnClickListener(null);
        holder.chip_action1.setOnClickListener(null);
    }

    protected AlarmItemAdapterListener listener;
    public void setAlarmClockAdapterListener( AlarmItemAdapterListener listener ) {
        this.listener = listener;
    }

    private View.OnClickListener showAlarmTypeMenu()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlarmTypeMenu(getActivity(), item, v);
            }
        };
    }

    private View.OnClickListener showOverflowMenu()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOverflowMenu(getActivity(), item, v);
            }
        };
    }

    private View.OnClickListener pickLabel()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRequestLabel(item);
                }
            }
        };
    }

    private View.OnClickListener pickOffset()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRequestOffset(item);
                }
            }
        };
    }

    private View.OnClickListener pickEvent()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRequestSolarEvent(item);
                }
            }
        };
    }

    private View.OnClickListener pickLocation()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRequestLocation(item);
                }
            }
        };
    }

    private View.OnClickListener pickRepeating()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRequestRepetition(item);
                }
            }
        };
    }

    private View.OnClickListener pickRingtone()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRequestRingtone(item);
                }
            }
        };
    }

    private CompoundButton.OnCheckedChangeListener pickVibrating()
    {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked && !item.vibrate)
                {
                    Context context = getActivity();
                    Vibrator vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrate != null) {
                        vibrate.vibrate(500);
                    }
                }
                item.vibrate = isChecked;
                item.modified = true;
            }
        };
    }

    private View.OnClickListener pickAction(final int actionNum)
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRequestAction(item, actionNum);
                }
            }
        };
    }

}
