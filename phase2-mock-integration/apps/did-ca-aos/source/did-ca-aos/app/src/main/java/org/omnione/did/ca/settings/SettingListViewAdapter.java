/*
 * Copyright 2024 OmniOne.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnione.did.ca.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.omnione.did.ca.R;

import java.util.ArrayList;

public class SettingListViewAdapter extends BaseAdapter {

    ArrayList<SettingData> listview_data = new ArrayList<SettingData>();
    @Override
    public int getCount() {
        return listview_data.size();
    }
    @Override
    public Object getItem(int position) {
        return listview_data.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_item, parent, false);
        }

        TextView itemName = (TextView) convertView.findViewById(R.id.name);
        TextView itemSubName = (TextView) convertView.findViewById(R.id.subName);

        SettingData settingData = listview_data.get(position);

        itemName.setText(settingData.getName());
        itemSubName.setText(settingData.getSubName());

        return convertView;
    }

    public void addItem(String name, String subName) {
        SettingData settingData = new SettingData();
        settingData.setName(name);
        settingData.setSubName(subName);
        listview_data.add(settingData);
    }
    public void removeAll() {
        listview_data.clear();
    }
}
