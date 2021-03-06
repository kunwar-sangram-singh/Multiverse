package org.multiverse.stms.gamma.transactionalobjects.txnlong;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaTxnLong;
import org.multiverse.stms.gamma.transactions.GammaTxn;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertOrecValue;
import static org.multiverse.api.TxnThreadLocal.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaTxnLong_atomicGetTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTxn();
    }

    @Test(expected = LockedException.class)
    public void whenUnconstructed() {
        GammaTxn tx = stm.newDefaultTxn();
        GammaTxnLong ref = new GammaTxnLong(tx);
        ref.atomicGet();
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        GammaTxnLong ref = new GammaTxnLong(stm, 100);

        GammaTxn tx = stm.newDefaultTxn();
        setThreadLocalTxn(tx);
        ref.set(10);

        assertEquals(100, ref.atomicGet());

        assertSame(tx, getThreadLocalTxn());
    }

    @Test
    public void whenUpdatedBiasedOnUnlocked() {
        GammaTxnLong ref = new GammaTxnLong(stm, 100);

        long result = ref.atomicGet();
        assertEquals(100, result);
        //assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdateBiasedAndPrivatizedByOther_thenLockedException() {
        GammaTxnLong ref = new GammaTxnLong(stm, 100);
        long version = ref.getVersion();

        GammaTxn otherTx = stm.newDefaultTxn();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        try {
            ref.atomicGet();
            fail();
        } catch (LockedException ex) {
        }

        assertSurplus(ref, 1);
        assertWriteBiased(ref);
        assertRefHasExclusiveLock(ref, otherTx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenUpdateBiasedAndEnsuredByOther() {
        GammaTxnLong ref = new GammaTxnLong(stm, 100);
        long version = ref.getVersion();

        GammaTxn otherTx = stm.newDefaultTxn();
        ref.getLock().acquire(otherTx, LockMode.Write);

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSurplus(ref, 1);
        assertRefHasWriteLock(ref, otherTx);
        assertWriteBiased(ref);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        GammaTxnLong ref = makeReadBiased(new GammaTxnLong(stm, 100));

        long result = ref.atomicGet();
        assertEquals(100, result);
        assertReadBiased(ref);
    }

    @Test
    public void whenReadBiasedAndPrivatizedByOther_thenLockedException() {
        GammaTxnLong ref = makeReadBiased(new GammaTxnLong(stm, 100));
        long version = ref.getVersion();

        GammaTxn otherTx = stm.newDefaultTxn();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        long orecValue = ref.orec;
        try {
            ref.atomicGet();
            fail();
        } catch (LockedException ex) {
        }

        assertOrecValue(ref, orecValue);
        assertRefHasExclusiveLock(ref, otherTx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenReadBiasedAndEnsuredByOther_thenLockedException() {
        GammaTxnLong ref = makeReadBiased(new GammaTxnLong(stm, 100));
        long version = ref.getVersion();

        GammaTxn otherTx = stm.newDefaultTxn();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        long orecValue = ref.orec;
        try {
            ref.atomicGet();
            fail();
        } catch (LockedException expected) {
        }

        assertOrecValue(ref,orecValue);
        assertVersionAndValue(ref, version, 100);
    }
}
