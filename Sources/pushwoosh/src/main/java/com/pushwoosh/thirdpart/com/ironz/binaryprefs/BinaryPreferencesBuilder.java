/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.thirdpart.com.ironz.binaryprefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.candidates.CacheCandidateProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.candidates.ConcurrentCacheCandidateProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.provider.CacheProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.cache.provider.ConcurrentCacheProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.encryption.KeyEncryption;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.encryption.ValueEncryption;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.event.BroadcastEventBridge;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.event.EventBridge;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.event.ExceptionHandler;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.event.MainThreadEventBridge;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.exception.PreferencesInitializationException;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.adapter.FileAdapter;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.adapter.NioFileAdapter;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.directory.AndroidDirectoryProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.directory.DirectoryProvider;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction.FileTransaction;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.file.transaction.MultiProcessTransaction;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.init.EagerFetchStrategy;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.init.FetchStrategy;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.init.LazyFetchStrategy;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.lock.LockFactory;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.lock.SimpleLockFactory;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.migration.MigrateProcessor;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.SerializerFactory;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.Persistable;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.serialization.serializer.persistable.PersistableRegistry;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.task.ScheduledBackgroundTaskExecutor;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.task.TaskExecutor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Class for building preferences instance.
 */
@SuppressWarnings("unused")
public final class BinaryPreferencesBuilder {

	private static final String INCORRECT_THREAD_INIT_MESSAGE = "Preferences should be instantiated in the main thread.";
	private static final String IPC_MODE_WITH_LAZY_MESSAGE = "IPC mode can't be used with lazy in-memory cache strategy!";

	/**
	 * Default name of preferences which name has not been defined.
	 */
	@SuppressWarnings("WeakerAccess")
	public static final String DEFAULT_NAME = "default";

	private final ParametersProvider parametersProvider = new ParametersProvider();

	private final Map<String, ReadWriteLock> locks = parametersProvider.getLocks();
	private final Map<String, Lock> processLocks = parametersProvider.getProcessLocks();
	private final Map<String, ExecutorService> executors = parametersProvider.getExecutors();
	private final Map<String, Map<String, Object>> caches = parametersProvider.getCaches();
	private final Map<String, Set<String>> cacheCandidates = parametersProvider.getCacheCandidates();
	private final Map<String, List<SharedPreferences.OnSharedPreferenceChangeListener>> allListeners = parametersProvider.getAllListeners();

	private final Context context;
	private final PersistableRegistry persistableRegistry = new PersistableRegistry();
	private final MigrateProcessor migrateProcessor = new MigrateProcessor();

	private File baseDir;
	private String name = DEFAULT_NAME;
	private boolean supportInterProcess = false;
	private boolean lazyMemoryCache = true;
	private KeyEncryption keyEncryption = KeyEncryption.NO_OP;
	private ValueEncryption valueEncryption = ValueEncryption.NO_OP;
	private ExceptionHandler exceptionHandler = ExceptionHandler.PRINT;

	/**
	 * Creates builder with base parameters.
	 * <p>
	 * Note: Please, use only one instance of preferences by name,
	 * this saves you from non-reasoned allocations.
	 * </p>
	 *
	 * @param context target context. Using application context is
	 *                very much appreciated
	 */
	public BinaryPreferencesBuilder(Context context) {
		this.context = context;
		this.baseDir = context.getFilesDir();
	}

	/**
	 * Defines preferences name for build instance.
	 *
	 * @param name target preferences name. Default name is {@link #DEFAULT_NAME}
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Defines usage of external directory for preferences saving.
	 * Default value is {@code false}.
	 *
	 * @param value all data will be saved inside external cache directory
	 *              if <code>true</code> value is passed
	 *              ({@link Context#getExternalFilesDir(String)}),
	 *              if <code>false</code> - will use standard app cache directory
	 *              ({@link Context#getFilesDir()}).
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder externalStorage(boolean value) {
		this.baseDir = value ? context.getExternalFilesDir(null) : context.getFilesDir();
		return this;
	}

	/**
	 * * Defines usage of custom directory for preferences saving.
	 * Be careful: write into external directory required appropriate
	 * runtime and manifest permissions.
	 *
	 * @param baseDir base directory for saving.
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder customDirectory(File baseDir) {
		this.baseDir = baseDir;
		return this;
	}

	/**
	 * Defines usage of IPC mechanism for delivering key updates and cache consistency.
	 * Default value is {@code false}.
	 * <p>
	 * Note: Please, note that one key change delta should be less than 1 (one) megabyte
	 * because IPC data transferring is limited by this capacity.
	 * </p>
	 *
	 * @param value {@code true} if would use IPC, {@code false} otherwise
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder supportInterProcess(boolean value) {
		this.supportInterProcess = value;
		return this;
	}

	/**
	 * Defines usage of lazy in-memory cache fetching mechanism for improving initialization speed.
	 * Default value is {@code true}.
	 *
	 * @param value {@code true} if would use lazy, {@code false} otherwise
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder lazyMemoryCache(boolean value) {
		this.lazyMemoryCache = value;
		return this;
	}

	/**
	 * Defines key encryption implementation which performs vice versa byte encryption operations.
	 * Default value is {@link KeyEncryption#NO_OP}
	 *
	 * @param keyEncryption key encryption implementation
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder keyEncryption(KeyEncryption keyEncryption) {
		this.keyEncryption = keyEncryption;
		return this;
	}

	/**
	 * Defines value encryption implementation which performs vice versa byte encryption operations.
	 * Default value is {@link ValueEncryption#NO_OP}
	 *
	 * @param valueEncryption value encryption implementation
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder valueEncryption(ValueEncryption valueEncryption) {
		this.valueEncryption = valueEncryption;
		return this;
	}

	/**
	 * Defines exception handler implementation which handles exception events e.g. logging operations.
	 * Default value is {@link ExceptionHandler#PRINT}
	 *
	 * @param exceptionHandler exception handler implementation
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder exceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	/**
	 * Registers {@link Persistable} data-object for de/serialization process.
	 * All {@link Persistable} data-objects should be registered for understanding
	 * de/serialization contract during cache initialization.
	 *
	 * @param key         target key which uses for fetching {@link Persistable}
	 *                    in {@link PreferencesEditor#putPersistable(String, Persistable)} method
	 * @param persistable target class type which implements {@link Persistable} interface
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder registerPersistable(String key, Class<? extends Persistable> persistable) {
		persistableRegistry.register(key, persistable);
		return this;
	}

	/**
	 * Performs migration from any implementation of preferences
	 * to this implementation.
	 * Appropriate transaction will be created for all migrated
	 * values. After successful migration all data in migrated
	 * preferences will be removed.
	 * Please note that all existing values in this implementation
	 * will be rewritten to values which migrates into. Also type
	 * information will be rewritten and lost too without any
	 * exception.
	 * If this method will be called multiple times for two or more
	 * different instances of preferences which has keys collision
	 * then last preferences values will be applied.
	 *
	 * @param preferences any implementation for migration.
	 * @return current builder instance
	 */
	public BinaryPreferencesBuilder migrateFrom(SharedPreferences preferences) {
		migrateProcessor.add(preferences);
		return this;
	}

	/**
	 * Builds preferences instance with predefined or default parameters.
	 * This method will fails if invocation performed not in the main thread.
	 *
	 * @return preferences instance with predefined or default parameters.
	 * @see PreferencesInitializationException
	 */
	public Preferences build() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new PreferencesInitializationException(INCORRECT_THREAD_INIT_MESSAGE);
		}
		if (lazyMemoryCache && supportInterProcess) {
			throw new UnsupportedOperationException(IPC_MODE_WITH_LAZY_MESSAGE);
		}
		BinaryPreferences preferences = createInstance();
		migrateProcessor.migrateTo(preferences);
		return preferences;
	}

	private BinaryPreferences createInstance() {

		DirectoryProvider directoryProvider = new AndroidDirectoryProvider(name, baseDir);
		FileAdapter fileAdapter = new NioFileAdapter(directoryProvider);
		LockFactory lockFactory = new SimpleLockFactory(name, directoryProvider, locks, processLocks);
		FileTransaction fileTransaction = new MultiProcessTransaction(fileAdapter, lockFactory, keyEncryption, valueEncryption);
		CacheCandidateProvider cacheCandidateProvider = new ConcurrentCacheCandidateProvider(name, cacheCandidates);
		CacheProvider cacheProvider = new ConcurrentCacheProvider(name, caches);
		TaskExecutor taskExecutor = new ScheduledBackgroundTaskExecutor(name, exceptionHandler, executors);
		SerializerFactory serializerFactory = new SerializerFactory(persistableRegistry);
		EventBridge eventsBridge = supportInterProcess ? new BroadcastEventBridge(
				context,
				name,
				cacheCandidateProvider,
				cacheProvider,
				serializerFactory,
				taskExecutor,
				valueEncryption,
				directoryProvider,
				allListeners
		) : new MainThreadEventBridge(name, allListeners);

		FetchStrategy fetchStrategy = lazyMemoryCache ? new LazyFetchStrategy(
				lockFactory,
				taskExecutor,
				cacheCandidateProvider,
				cacheProvider,
				fileTransaction,
				serializerFactory
		) : new EagerFetchStrategy(
				lockFactory,
				taskExecutor,
				cacheCandidateProvider,
				cacheProvider,
				fileTransaction,
				serializerFactory
		);

		return new BinaryPreferences(
				fileTransaction,
				eventsBridge,
				cacheCandidateProvider,
				cacheProvider,
				taskExecutor,
				serializerFactory,
				lockFactory,
				fetchStrategy
		);
	}
}
