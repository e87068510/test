package com.itri.uschart;


import android.content.Intent;
import android.hardware.usb.UsbDevice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.itri.uschart.Activity.USChartActivity;

import java.util.List;

//import com.bnhsu.androidusbhost.service.UsbHostService;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemHolder> {

    private List<UsbDevice> usbDeviceList;

    public ItemAdapter(List<UsbDevice> usbDeviceList) {
        this.usbDeviceList = usbDeviceList;
    }

    @NonNull
    @Override
    public ItemAdapter.ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemAdapter.ItemHolder holder, int position) {
        final UsbDevice device = usbDeviceList.get(position);

        String s = device.getDeviceName() + " - " + device.getProductName() + " - " + device.getManufacturerName() +" - PID: " +device.getProductId() + " - VID: " + device.getDeviceId();
        /*
        " - " + device.getSerialNumber()+
                "\n - Configuration Count:" + device.getConfigurationCount() + " - Configuration:" + device.getConfiguration(0).describeContents() +
                "\n - Interface Count: " + device.getInterfaceCount() + "Interface describeContents: "+ device.getInterface(0).describeContents() +
                "\n - Interface(0)'s Endpoint Count: " + device.getInterface(0).getEndpointCount();
        */
                /*+
                "\n - Endpoint(1): " + device.getInterface(0).getEndpoint(1).describeContents() + "Direction: " + device.getInterface(0).getEndpoint(1).getDirection();
                */
                /*+ "Direction: " + device.getInterface(0).getEndpoint(0).getDirection();
        /*+
                "\n - Endpoint(0): " + device.getInterface(0).getEndpoint(1).describeContents() + "Direction: " + device.getInterface(0).getEndpoint(1).getDirection();
*/
        holder.textView.setText(s);
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Communicate with it
                Intent intent = new Intent(view.getContext(), USChartActivity.class);
                intent.putExtra("Device", device);
                view.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usbDeviceList.size();
    }

    class ItemHolder extends RecyclerView.ViewHolder {

        LinearLayout layout;
        TextView textView;

        ItemHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.linear_layout);
            textView = itemView.findViewById(R.id.device_name);
        }
    }
}
