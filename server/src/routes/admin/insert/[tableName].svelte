<script context="module">
  import { getPreloadApiResponse } from "requests";

  export async function preload(page, session) {
    const model = await getPreloadApiResponse(
      `/admin/api/getModel/${page.params.tableName}`,
      {},
      this
    );

    if (model.hasOwnProperty("tableName")) {
      return model;
    }

    this.error(404, "Таких моделей у нас нет.");
  }
</script>

<script>
  import { ListItem } from "svelte-materialify/src";
  import TextField from "Textfield.svelte";
  import Title from "Title.svelte";
  import Button from "Button.svelte";
  import {postApi} from "requests";

  export let columns;
  export let tableName;

  let columnValues = {};
  let columnErrors = {};

  for (const key of Object.keys(columns)) {
    columnValues[key] = "";
    columnErrors[key] = false;
  }

  const submit = () => {
    for (const key of Object.keys(columnErrors)) {
      if (columnErrors[key] !== false) {
        return;
      }
    }

    postApi(`/admin/api/insert/${tableName}`, columnValues, true)
      .then(res => {
        console.log(res);
      }, e => {
        console.log(e);
      });
  };
</script>

<style lang="scss">
  .insert-header {
    text-align: center;
  }

  :global {
    .s-list-item__title {
      padding: 1rem 0.5rem 0;
    }

    .insert-submit {
      margin: 0.25rem 0 0 1.5rem;
    }
  }
</style>

<Title caption={tableName} />

<h2 class="insert-header">{tableName}</h2>

{#each Object.keys(columns) as key}
  <ListItem class="insert-list-item">
    {#if columns[key].type === 'string'}
      <TextField
        bind:value={columnValues[key]}
        counter={columns[key].length}
        label={key} />
    {/if}
  </ListItem>
{/each}

<Button on:click={submit} class="insert-submit" text label="Создать" />
