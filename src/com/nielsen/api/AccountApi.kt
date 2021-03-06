package com.nielsen.api

import com.nielsen.model.Account
import com.nielsen.model.Amount
import com.nielsen.model.DeduplicationId
import com.nielsen.service.AccountService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import java.math.BigDecimal
import java.util.UUID
import org.kodein.di.Kodein
import org.kodein.di.generic.instance

fun Route.accountApi(kodein: Kodein) {
    val accountService by kodein.instance<AccountService>()

    route("/accounts") {
        post("/") {
            val createAccountRequest = call.receive(CreateAccountRequest::class)

            val account = accountService.insert(Account(Account.Id(createAccountRequest.id)))

            call.respond(HttpStatusCode.Created, AccountResponse.from(account))
        }

        get("/{accountId}") {
            val accountId = Account.Id(UUID.fromString(call.parameters["accountId"]))

            val account = accountService.getAccountById(accountId)

            call.respond(HttpStatusCode.OK, AccountResponse.from(account))
        }

        post("/{accountId}/deposit") {
            val accountId = Account.Id(UUID.fromString(call.parameters["accountId"]))
            val depositRequest = call.receive(DepositRequest::class)

            val accountData = accountService.deposit(
                accountId,
                Amount.from(depositRequest.amount),
                DeduplicationId(depositRequest.deduplicationId)
            )

            call.respond(HttpStatusCode.OK, AccountResponse.from(accountData.account))
        }

        post("/{accountId}/withdraw") {
            val accountId = Account.Id(UUID.fromString(call.parameters["accountId"]))
            val withdrawRequest = call.receive(WithdrawRequest::class)

            val accountData = accountService.withdraw(
                accountId,
                Amount.from(withdrawRequest.amount),
                DeduplicationId(withdrawRequest.deduplicationId)
            )

            call.respond(HttpStatusCode.OK, AccountResponse.from(accountData.account))
        }

        post("/{accountId}/transfer") {
            val accountId = Account.Id(UUID.fromString(call.parameters["accountId"]))
            val transferRequest = call.receive(TransferRequest::class)

            val accountData = accountService.transfer(
                sourceAccountId = accountId,
                destinationAccountId = Account.Id(transferRequest.destinationAccountId),
                amount = Amount.from(transferRequest.amount),
                deduplicationId = DeduplicationId(transferRequest.deduplicationId)
            )

            call.respond(HttpStatusCode.OK, AccountResponse.from(accountData.account))
        }
    }
}

data class CreateAccountRequest(val id: UUID)

data class DepositRequest(val amount: BigDecimal, val deduplicationId: UUID)

data class WithdrawRequest(val amount: BigDecimal, val deduplicationId: UUID)

data class TransferRequest(val destinationAccountId: UUID, val amount: BigDecimal, val deduplicationId: UUID)

data class AccountResponse(val id: UUID, val balance: BigDecimal) {
    companion object {
        fun from(account: Account): AccountResponse {
            return AccountResponse(
                id = account.id.value,
                balance = account.balance.value.setScale(2)
            )
        }
    }
}
