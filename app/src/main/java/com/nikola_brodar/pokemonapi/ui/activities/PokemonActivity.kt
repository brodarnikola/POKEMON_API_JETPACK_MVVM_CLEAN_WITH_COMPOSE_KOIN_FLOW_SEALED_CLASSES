package com.nikola_brodar.pokemonapi.ui.activities

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.nikola_brodar.domain.ResultState
import com.nikola_brodar.domain.model.MainPokemon
import com.nikola_brodar.pokemonapi.R
import com.nikola_brodar.pokemonapi.databinding.ActivityPokemonBinding
import com.nikola_brodar.pokemonapi.ui.adapters.PokemonAdapter
import com.nikola_brodar.pokemonapi.viewmodels.PokemonViewModel
import com.nikola_brodar.pokemonapi.ui.utilities.hide
import com.nikola_brodar.pokemonapi.ui.utilities.show
import kotlinx.android.synthetic.main.activity_pokemon.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class PokemonActivity : BaseActivity(R.id.no_internet_layout) {

    var displayCurrentPokemonData = false

    val pokemonViewModel: PokemonViewModel by viewModel()

    private lateinit var pokemonAdapter: PokemonAdapter
    var pokemonLayoutManager: LinearLayoutManager? = null

    private lateinit var binding: ActivityPokemonBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPokemonBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(findViewById(R.id.toolbarWeather))
        supportActionBar?.setDisplayShowTitleEnabled(false)

        displayCurrentPokemonData = intent.getBooleanExtra("displayCurrentPokemonData", false)
    }

    override fun onStart() {
        super.onStart()
        viewLoaded = true

        initializeUi()

        pokemonViewModel.mainPokemonData.observe(this, Observer { items ->

            when( items ) {
                is ResultState.Loading -> {
                    showProgressBar()
                    hideAllUiElements()
                }
                is ResultState.Success -> {
                    clearAdapter()
                    hideProgressBar()
                    displayAllUiElements()
                    successUpdateUiWithData(items.data as MainPokemon)
                }
                is ResultState.Error -> {
                    hideProgressBar()
                    displayAllUiElements()
                    somethingWentWrong(items)
                }
            }
        })

        if( displayCurrentPokemonData )
            pokemonViewModel.getAllPokemonDataFromLocalStorage()
        else
            pokemonViewModel.getPokemonData()

        floatingButton.setOnClickListener {
            showProgressBar()
            hideAllUiElements()
            pokemonViewModel.getPokemonData()
        }
    }

    private fun somethingWentWrong(items: ResultState.Error) {
        showSnackbarSync( items.message + items.exception.toString(), true, binding.mainLayout )
    }

    private fun clearAdapter() {
        pokemonAdapter.pokemonStatsList.clear()
        pokemonAdapter.notifyDataSetChanged()
    }

    private fun successUpdateUiWithData(pokemonData: MainPokemon) {

        Log.d(ContentValues.TAG, "Data is: ${pokemonData.stats.joinToString { "-" + it.stat.name }}")

        tvName.text = "Name: " + pokemonData.name

        Glide.with(this)
            .load( pokemonData.sprites.backDefault)
            .placeholder(R.drawable.garden_tab_selector)
            .error(R.drawable.garden_tab_selector)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(ivBack)

        Glide.with(this)
            .load( pokemonData.sprites.frontDefault)
            .placeholder(R.drawable.garden_tab_selector)
            .error(R.drawable.garden_tab_selector)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(ivFront)

        pokemonAdapter.updateDevices(pokemonData.stats.toMutableList())
    }

    private fun hideProgressBar() {
        progressBar.hide()
    }

    private fun showProgressBar() {
        progressBar.show()
    }

    private fun hideAllUiElements() {
        binding.tvName.visibility = View.GONE
        binding.ivBack.visibility = View.GONE
        binding.ivFront.visibility = View.GONE
        binding.pokemonList.visibility = View.GONE
    }

    private fun displayAllUiElements() {
        binding.tvName.visibility = View.VISIBLE
        binding.ivBack.visibility = View.VISIBLE
        binding.ivFront.visibility = View.VISIBLE
        binding.pokemonList.visibility = View.VISIBLE
    }

    private fun initializeUi() {

        pokemonLayoutManager = LinearLayoutManager(this@PokemonActivity, RecyclerView.VERTICAL, false)

        pokemonAdapter = PokemonAdapter( mutableListOf() )

        binding.pokemonList.apply {
            layoutManager = pokemonLayoutManager
            adapter = pokemonAdapter
        }
        binding.pokemonList.adapter = pokemonAdapter

        binding.btnRoomOldWeatherData.setOnClickListener {
            val intent = Intent(this, PokemonMovesActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    override fun onNetworkStateUpdated(available: Boolean) {
        super.onNetworkStateUpdated(available)
        if( viewLoaded == true )
            updateConnectivityUi()
    }

}