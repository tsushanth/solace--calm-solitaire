package com.factory.solacecalmsolitaire.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GameResultDao_Impl implements GameResultDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GameResultEntity> __insertionAdapterOfGameResultEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public GameResultDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGameResultEntity = new EntityInsertionAdapter<GameResultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `game_results` (`id`,`won`,`drawCount`,`score`,`moves`,`durationSeconds`,`completedAtEpochMillis`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GameResultEntity entity) {
        statement.bindLong(1, entity.getId());
        final int _tmp = entity.getWon() ? 1 : 0;
        statement.bindLong(2, _tmp);
        statement.bindLong(3, entity.getDrawCount());
        statement.bindLong(4, entity.getScore());
        statement.bindLong(5, entity.getMoves());
        statement.bindLong(6, entity.getDurationSeconds());
        statement.bindLong(7, entity.getCompletedAtEpochMillis());
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM game_results";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final GameResultEntity result,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGameResultEntity.insert(result);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GameResultEntity>> observeAll() {
    final String _sql = "SELECT * FROM game_results ORDER BY completedAtEpochMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_results"}, new Callable<List<GameResultEntity>>() {
      @Override
      @NonNull
      public List<GameResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfWon = CursorUtil.getColumnIndexOrThrow(_cursor, "won");
          final int _cursorIndexOfDrawCount = CursorUtil.getColumnIndexOrThrow(_cursor, "drawCount");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfMoves = CursorUtil.getColumnIndexOrThrow(_cursor, "moves");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfCompletedAtEpochMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAtEpochMillis");
          final List<GameResultEntity> _result = new ArrayList<GameResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameResultEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final boolean _tmpWon;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfWon);
            _tmpWon = _tmp != 0;
            final int _tmpDrawCount;
            _tmpDrawCount = _cursor.getInt(_cursorIndexOfDrawCount);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final int _tmpMoves;
            _tmpMoves = _cursor.getInt(_cursorIndexOfMoves);
            final int _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getInt(_cursorIndexOfDurationSeconds);
            final long _tmpCompletedAtEpochMillis;
            _tmpCompletedAtEpochMillis = _cursor.getLong(_cursorIndexOfCompletedAtEpochMillis);
            _item = new GameResultEntity(_tmpId,_tmpWon,_tmpDrawCount,_tmpScore,_tmpMoves,_tmpDurationSeconds,_tmpCompletedAtEpochMillis);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
