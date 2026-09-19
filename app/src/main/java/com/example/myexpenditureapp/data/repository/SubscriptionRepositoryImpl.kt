package com.example.myexpenditureapp.data.repository

import com.example.myexpenditureapp.data.dao.SubscriptionDao
import com.example.myexpenditureapp.data.entity.Subscription
import com.example.myexpenditureapp.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class SubscriptionRepositoryImpl(
    private val subscriptionDao: SubscriptionDao
) : SubscriptionRepository {

    override fun getAllSubscriptions(): Flow<List<Subscription>> =
        subscriptionDao.getAllSubscriptions()

    override fun getActiveSubscriptions(): Flow<List<Subscription>> =
        subscriptionDao.getActiveSubscriptions()

    override suspend fun getSubscriptionById(id: Long): Subscription? =
        subscriptionDao.getSubscriptionById(id)

    override suspend fun insertSubscription(subscription: Subscription): Long =
        subscriptionDao.insert(subscription)

    override suspend fun updateSubscription(subscription: Subscription) =
        subscriptionDao.update(subscription)

    override suspend fun deleteSubscription(subscription: Subscription) =
        subscriptionDao.delete(subscription)
}
