// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

// OpenZeppelin's audited, standard ERC-20 and ownership building blocks.
import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title MemeCoin  (TESTNET / EDUCATIONAL)
 * @notice A deliberately simple, honest ERC-20 meme token.
 *
 * Design choices, on purpose:
 *  - The ENTIRE supply is minted once, to the deployer, in the constructor.
 *  - There is NO hidden mint function, so the supply can never be inflated later.
 *  - There is NO transfer tax, blacklist, or pause switch — nothing that could be
 *    used to trap holders. This is the opposite of a "rug pull" contract.
 *
 * Ownable is included only so you can learn about access control; it grants NO
 * power over anyone's balance. Renouncing ownership (renounceOwnership()) is a
 * common trust signal if you ever take a token public.
 *
 * ⚠️ Intended for TEST NETWORKS ONLY. See README.md.
 */
contract MemeCoin is ERC20, Ownable {
    /**
     * @param name_          Full token name, e.g. "Awakening Coin".
     * @param symbol_        Ticker, e.g. "AWAKE".
     * @param initialSupply  Whole-token count (decimals are added automatically),
     *                       e.g. 1000000 for one million tokens.
     */
    constructor(
        string memory name_,
        string memory symbol_,
        uint256 initialSupply
    ) ERC20(name_, symbol_) Ownable(msg.sender) {
        _mint(msg.sender, initialSupply * 10 ** decimals());
    }
}
