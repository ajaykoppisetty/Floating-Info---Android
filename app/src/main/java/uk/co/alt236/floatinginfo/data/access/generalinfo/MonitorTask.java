/*
 * Copyright 2016 Alexandros Schillings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.alt236.floatinginfo.data.access.generalinfo;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import uk.co.alt236.floatinginfo.data.access.generalinfo.inforeader.cpu.CpuData;
import uk.co.alt236.floatinginfo.data.access.generalinfo.inforeader.cpu.CpuUtilisationReader;
import uk.co.alt236.floatinginfo.data.access.generalinfo.inforeader.fgappinfo.ForegroundAppData;
import uk.co.alt236.floatinginfo.data.access.generalinfo.inforeader.fgappinfo.ForegroundAppDiscovery;
import uk.co.alt236.floatinginfo.data.access.generalinfo.inforeader.memory.MemoryData;
import uk.co.alt236.floatinginfo.data.access.generalinfo.inforeader.memory.MemoryInfoReader;
import uk.co.alt236.floatinginfo.data.access.generalinfo.inforeader.network.NetDataReader;
import uk.co.alt236.floatinginfo.data.access.generalinfo.inforeader.network.model.NetData;
import uk.co.alt236.floatinginfo.util.Constants;

/*package*/ class MonitorTask {
    private final Context mContext;
    private InnerTask mTask;

    public MonitorTask(final Context context) {
        mContext = context.getApplicationContext();
    }

    public void start(@NonNull final UpdateCallback callback) {
        mTask = new InnerTask(mContext) {
            @Override
            protected void onProgressUpdate(final MonitorTask.MonitorUpdate... values) {
                callback.onUpdate(values[0]);
            }
        };

        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void stop() {
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    public interface UpdateCallback {
        void onUpdate(MonitorUpdate update);
    }

    private static class InnerTask extends AsyncTask<Void, MonitorTask.MonitorUpdate, Void> {

        private final ForegroundAppDiscovery mForegroundAppDiscovery;
        private final NetDataReader mNetDataReader;
        private final CpuUtilisationReader mCpuUtilisationReader;
        private final MemoryInfoReader mMemoryInfoReader;

        public InnerTask(final Context context) {
            mForegroundAppDiscovery = new ForegroundAppDiscovery(context);
            mNetDataReader = new NetDataReader(context);
            mMemoryInfoReader = new MemoryInfoReader(context);
            mCpuUtilisationReader = new CpuUtilisationReader();
        }

        @Override
        protected Void doInBackground(final Void... voids) {

            while (!isCancelled()) {
                mNetDataReader.update();
                mCpuUtilisationReader.update();

                final ForegroundAppData appData = mForegroundAppDiscovery.getForegroundApp();
                mMemoryInfoReader.update(appData.getPid());

                publishProgress(
                        new MonitorUpdate(
                                appData,
                                mNetDataReader.getNetData(),
                                mMemoryInfoReader.getInfo(),
                                mCpuUtilisationReader.getCpuInfo()));

                try {
                    Thread.sleep(Constants.PROC_MONITOR_SLEEP);
                } catch (final InterruptedException e) {
                    // NOOP
                }
            }

            return null;
        }
    }

    public static class MonitorUpdate {
        private final ForegroundAppData mForegroundAppData;
        private final NetData mNetData;
        private final MemoryData mMemoryData;
        private final CpuData mCpuData;

        public MonitorUpdate(final ForegroundAppData appData,
                             final NetData netData,
                             final MemoryData info,
                             final CpuData cpuInfo) {

            mForegroundAppData = appData;
            mNetData = netData;
            mMemoryData = info;
            mCpuData = cpuInfo;
        }

        public ForegroundAppData getForegroundAppData() {
            return mForegroundAppData;
        }

        public NetData getNetData() {
            return mNetData;
        }

        public MemoryData getMemoryData() {
            return mMemoryData;
        }

        public CpuData getCpuData() {
            return mCpuData;
        }
    }
}